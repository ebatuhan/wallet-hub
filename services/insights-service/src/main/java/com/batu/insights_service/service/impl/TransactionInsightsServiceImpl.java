package com.batu.insights_service.service.impl;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.time.Year;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.batu.insights_service.dto.SpendingCategoryAggregate;
import com.batu.insights_service.dto.SpendingGraphAggregate;
import com.batu.insights_service.dto.SpendingPeriod;
import com.batu.insights_service.dto.SpendingPeriod.SpendingMonth;
import com.batu.insights_service.dto.SpendingPeriod.SpendingYear;
import com.batu.insights_service.entity.TransactionInsightRow;
import com.batu.insights_service.repository.TransactionInsightsRepository;
import com.batu.insights_service.service.TransactionInsightsService;
import com.batu.shared.dto.response.IncomeSummaryResponseDto;
import com.batu.shared.dto.response.SpendingCurrencyGroupDto;
import com.batu.shared.dto.response.SpendingGraphPointDto;
import com.batu.shared.dto.response.SpendingGraphResponseDto;
import com.batu.shared.dto.response.SpendingPerCategoryByAccountResponseDto;
import com.batu.shared.dto.response.SpendingPerCategoryDto;
import com.batu.shared.dto.response.SpendingPerCategoryResponseDto;
import com.batu.shared.dto.response.SpendingGraphSeriesDto;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TransactionInsightsServiceImpl implements TransactionInsightsService {

    private final TransactionInsightsRepository transactionInsightsRepository;

    @Override
    public SpendingPerCategoryResponseDto getSpendingByCategory(String from, Jwt principal) {
        return getSpendingByCategory(from, UUID.fromString(principal.getSubject()));
    }

    @Override
    public SpendingPerCategoryResponseDto getSpendingByCategory(String from, UUID userId) {
        SpendingPeriod period = parseSpendingPeriod(from);
        DateRange range = monthRange(period);
        List<SpendingCategoryAggregate> categories = transactionInsightsRepository.findSpendingByMonths(
                range.from(),
                range.to(),
                userId);
        return new SpendingPerCategoryResponseDto(userId, groupSpendingByCurrency(categories));
    }

    @Override
    public SpendingGraphResponseDto getSpendingGraph(String from, Jwt principal) {
        return getSpendingGraph(from, UUID.fromString(principal.getSubject()));
    }

    @Override
    public SpendingGraphResponseDto getSpendingGraph(String from, UUID userId) {
        SpendingPeriod period = parseSpendingPeriod(from);
        GraphResult graph = loadGraph(period, userId, null);
        return new SpendingGraphResponseDto(userId, null, graph.groupBy(), graph.from(), graph.to(), graph.series());
    }

    @Override
    public SpendingGraphResponseDto getSpendingGraphByAccount(String from, UUID accountId, Jwt principal) {
        return getSpendingGraphByAccount(from, accountId, UUID.fromString(principal.getSubject()));
    }

    @Override
    public SpendingGraphResponseDto getSpendingGraphByAccount(String from, UUID accountId, UUID userId) {
        SpendingPeriod period = parseSpendingPeriod(from);
        GraphResult graph = loadGraph(period, userId, accountId);
        return new SpendingGraphResponseDto(userId, accountId, graph.groupBy(), graph.from(), graph.to(), graph.series());
    }

    @Override
    public SpendingPerCategoryByAccountResponseDto getSpendingPerCategoryByAccount(String from, UUID accountId,
            Jwt principal) {
        return getSpendingPerCategoryByAccount(from, accountId, UUID.fromString(principal.getSubject()));
    }

    @Override
    public SpendingPerCategoryByAccountResponseDto getSpendingPerCategoryByAccount(String from, UUID accountId,
            UUID userId) {
        SpendingPeriod period = parseSpendingPeriod(from);
        DateRange range = monthRange(period);
        List<SpendingCategoryAggregate> categories = transactionInsightsRepository.findSpendingByMonthsAndAccount(
                range.from(),
                range.to(),
                userId,
                accountId);
        return new SpendingPerCategoryByAccountResponseDto(
                userId,
                accountId,
                groupSpendingByCurrency(categories));
    }

    @Override
    public IncomeSummaryResponseDto getIncome(Date from, Date to, Jwt principal) {
        return getIncome(from, to, UUID.fromString(principal.getSubject()));
    }

    @Override
    public IncomeSummaryResponseDto getIncome(Date from, Date to, UUID userId) {
        validateDateRange(from, to);
        return new IncomeSummaryResponseDto(userId, transactionInsightsRepository.findIncomeByInterval(from, to, userId));
    }

    @Override
    public void save(TransactionInsightRow transactionInsightRow) {
        transactionInsightsRepository.save(transactionInsightRow);
    }

    @Override
    public void removeAccountTransactions(UUID accountId, UUID userId) {
        transactionInsightsRepository.deleteByAccount(accountId, userId);
    }

    private void validateDateRange(Date from, Date to) {
        if (from.after(to)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "'from' date must be before or equal to 'to' date");
        }
    }

    private SpendingPeriod parseSpendingPeriod(String from) {
        if (from == null || from.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "'from' must be YYYY or YYYY-MM");
        }

        try {
            if (from.length() == 7) {
                return new SpendingMonth(YearMonth.parse(from));
            }
            return new SpendingYear(Year.parse(from));
        } catch (DateTimeParseException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "'from' must be YYYY or YYYY-MM", ex);
        }
    }

    private DateRange monthRange(SpendingPeriod period) {
        if (period instanceof SpendingYear spendingYear) {
            Year year = spendingYear.year();
            return new DateRange(
                    Date.valueOf(year.atMonth(1).atDay(1)),
                    Date.valueOf(year.atMonth(12).atDay(1)),
                    year.atMonth(1).atDay(1),
                    year.atMonth(12).atEndOfMonth());
        }

        YearMonth month = ((SpendingMonth) period).month();
        return new DateRange(
                Date.valueOf(month.atDay(1)),
                Date.valueOf(month.atDay(1)),
                month.atDay(1),
                month.atEndOfMonth());
    }

    private GraphResult loadGraph(SpendingPeriod period, UUID userId, UUID accountId) {
        if (period instanceof SpendingYear) {
            DateRange range = monthRange(period);
            List<SpendingGraphAggregate> points = accountId == null
                    ? transactionInsightsRepository.findMonthlySpendingGraph(range.from(), range.to(), userId)
                    : transactionInsightsRepository.findMonthlySpendingGraphByAccount(range.from(), range.to(), userId, accountId);
            return new GraphResult(
                    "MONTH",
                    range.responseFrom(),
                    range.responseTo(),
                    fillMissingSeries(points, range.responseFrom(), range.responseTo(), GraphBucket.MONTH));
        }

        DateRange range = monthRange(period);
        LocalDate firstBucket = alignWeek(range.responseFrom());
        LocalDate lastBucket = alignWeek(range.responseTo());
        List<SpendingGraphAggregate> points = accountId == null
                ? transactionInsightsRepository.findWeeklySpendingGraph(range.from(), userId)
                : transactionInsightsRepository.findWeeklySpendingGraphByAccount(range.from(), userId, accountId);
        return new GraphResult(
                "WEEK",
                range.responseFrom(),
                range.responseTo(),
                fillMissingSeries(points, firstBucket, lastBucket, GraphBucket.WEEK));
    }

    private List<SpendingCurrencyGroupDto> groupSpendingByCurrency(List<SpendingCategoryAggregate> categories) {
        Map<String, List<SpendingCategoryAggregate>> categoriesByCurrency = categories.stream()
                .collect(Collectors.groupingBy(
                        SpendingCategoryAggregate::isoCurrencyCode,
                        LinkedHashMap::new,
                        Collectors.toList()));

        List<SpendingCurrencyGroupDto> groups = new ArrayList<>();
        for (Map.Entry<String, List<SpendingCategoryAggregate>> entry : categoriesByCurrency.entrySet()) {
            List<SpendingPerCategoryDto> categoryDtos = entry.getValue().stream()
                    .map(category -> new SpendingPerCategoryDto(
                            category.primaryCategoryId(),
                            category.percentage(),
                            category.totalAmount()))
                    .toList();
            BigDecimal totalSpent = entry.getValue().stream()
                    .map(SpendingCategoryAggregate::totalAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            groups.add(new SpendingCurrencyGroupDto(entry.getKey(), totalSpent, categoryDtos));
        }

        return groups;
    }

    private List<SpendingGraphSeriesDto> fillMissingSeries(List<SpendingGraphAggregate> points,
            LocalDate from,
            LocalDate to,
            GraphBucket bucket) {
        Map<String, List<SpendingGraphAggregate>> pointsByCurrency = points.stream()
                .collect(Collectors.groupingBy(
                        SpendingGraphAggregate::isoCurrencyCode,
                        LinkedHashMap::new,
                        Collectors.toList()));

        return pointsByCurrency.entrySet().stream()
                .map(entry -> new SpendingGraphSeriesDto(
                        entry.getKey(),
                        fillMissingPoints(entry.getValue(), from, to, bucket)))
                .sorted(Comparator.comparing(SpendingGraphSeriesDto::isoCurrencyCode))
                .toList();
    }

    private List<SpendingGraphPointDto> fillMissingPoints(List<SpendingGraphAggregate> points,
            LocalDate from,
            LocalDate to,
            GraphBucket graphBucket) {
        Map<LocalDate, SpendingGraphPointDto> pointsByBucket = points.stream()
                .collect(Collectors.toMap(
                        SpendingGraphAggregate::bucket,
                        point -> new SpendingGraphPointDto(point.bucket(), point.totalAmount()),
                        (left, right) -> new SpendingGraphPointDto(
                                left.bucket(),
                                left.amountSpent().add(right.amountSpent())),
                        LinkedHashMap::new));

        LocalDate bucket = from;
        java.util.ArrayList<SpendingGraphPointDto> filled = new java.util.ArrayList<>();

        while (!bucket.isAfter(to)) {
            SpendingGraphPointDto point = pointsByBucket.get(bucket);
            filled.add(point == null ? new SpendingGraphPointDto(bucket, BigDecimal.ZERO) : point);
            bucket = nextBucket(bucket, graphBucket);
        }

        return filled;
    }

    private LocalDate alignWeek(LocalDate date) {
        return date.minusDays(date.getDayOfWeek().getValue() - 1L);
    }

    private LocalDate nextBucket(LocalDate bucket, GraphBucket graphBucket) {
        return switch (graphBucket) {
            case WEEK -> bucket.plusWeeks(1);
            case MONTH -> bucket.plusMonths(1);
        };
    }

    private enum GraphBucket {
        WEEK,
        MONTH
    }

    private record DateRange(Date from, Date to, LocalDate responseFrom, LocalDate responseTo) {
    }

    private record GraphResult(String groupBy, LocalDate from, LocalDate to, List<SpendingGraphSeriesDto> series) {
    }
}
