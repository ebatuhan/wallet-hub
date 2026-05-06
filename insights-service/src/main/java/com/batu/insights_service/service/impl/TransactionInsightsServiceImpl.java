package com.batu.insights_service.service.impl;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
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
    public SpendingPerCategoryResponseDto getSpendingByCategory(Date from, Date to, Jwt principal) {
        return getSpendingByCategory(from, to, UUID.fromString(principal.getSubject()));
    }

    @Override
    public SpendingPerCategoryResponseDto getSpendingByCategory(Date from, Date to, UUID userId) {
        validateDateRange(from, to);
        List<SpendingCategoryAggregate> categories = transactionInsightsRepository.findByInterval(from, to, userId);
        return new SpendingPerCategoryResponseDto(userId, groupSpendingByCurrency(categories));
    }

    @Override
    public SpendingGraphResponseDto getSpendingGraph(Date from, Date to, Jwt principal) {
        return getSpendingGraph(from, to, UUID.fromString(principal.getSubject()));
    }

    @Override
    public SpendingGraphResponseDto getSpendingGraph(Date from, Date to, UUID userId) {
        validateDateRange(from, to);
        LocalDate fromDate = from.toLocalDate();
        LocalDate toDate = to.toLocalDate();
        GraphGranularity granularity = determineGranularity(fromDate, toDate);

        List<SpendingGraphSeriesDto> series = fillMissingSeries(
                transactionInsightsRepository.findSpendingGraphByInterval(from, to, userId, bucketExpression(granularity)),
                fromDate,
                toDate,
                granularity);

        return new SpendingGraphResponseDto(userId, null, granularity.name(), fromDate, toDate, series);
    }

    @Override
    public SpendingGraphResponseDto getSpendingGraphByAccount(Date from, Date to, UUID accountId, Jwt principal) {
        return getSpendingGraphByAccount(from, to, accountId, UUID.fromString(principal.getSubject()));
    }

    @Override
    public SpendingGraphResponseDto getSpendingGraphByAccount(Date from, Date to, UUID accountId, UUID userId) {
        validateDateRange(from, to);
        LocalDate fromDate = from.toLocalDate();
        LocalDate toDate = to.toLocalDate();
        GraphGranularity granularity = determineGranularity(fromDate, toDate);

        List<SpendingGraphSeriesDto> series = fillMissingSeries(
                transactionInsightsRepository.findSpendingGraphByIntervalAndAccount(
                        from,
                        to,
                        userId,
                        accountId,
                        bucketExpression(granularity)),
                fromDate,
                toDate,
                granularity);

        return new SpendingGraphResponseDto(userId, accountId, granularity.name(), fromDate, toDate, series);
    }

    @Override
    public SpendingPerCategoryByAccountResponseDto getSpendingPerCategoryByAccount(Date from, Date to, UUID accountId,
            Jwt principal) {
        return getSpendingPerCategoryByAccount(from, to, accountId, UUID.fromString(principal.getSubject()));
    }

    @Override
    public SpendingPerCategoryByAccountResponseDto getSpendingPerCategoryByAccount(Date from, Date to, UUID accountId,
            UUID userId) {
        validateDateRange(from, to);
        List<SpendingCategoryAggregate> categories = transactionInsightsRepository.findByIntervalAndAccount(
                from,
                to,
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

    private GraphGranularity determineGranularity(LocalDate from, LocalDate to) {
        long dayCount = ChronoUnit.DAYS.between(from, to) + 1;
        if (dayCount <= 31) {
            return GraphGranularity.DAY;
        }
        if (dayCount <= 180) {
            return GraphGranularity.WEEK;
        }
        return GraphGranularity.MONTH;
    }

    private String bucketExpression(GraphGranularity granularity) {
        return switch (granularity) {
            case DAY -> "toDate(latest_date)";
            case WEEK -> "toDate(toStartOfWeek(latest_date))";
            case MONTH -> "toDate(toStartOfMonth(latest_date))";
        };
    }

    private List<SpendingGraphSeriesDto> fillMissingSeries(List<SpendingGraphAggregate> points,
            LocalDate from,
            LocalDate to,
            GraphGranularity granularity) {
        Map<String, List<SpendingGraphAggregate>> pointsByCurrency = points.stream()
                .collect(Collectors.groupingBy(
                        SpendingGraphAggregate::isoCurrencyCode,
                        LinkedHashMap::new,
                        Collectors.toList()));

        return pointsByCurrency.entrySet().stream()
                .map(entry -> new SpendingGraphSeriesDto(
                        entry.getKey(),
                        fillMissingPoints(entry.getValue(), from, to, granularity)))
                .sorted(Comparator.comparing(SpendingGraphSeriesDto::isoCurrencyCode))
                .toList();
    }

    private List<SpendingGraphPointDto> fillMissingPoints(List<SpendingGraphAggregate> points,
            LocalDate from,
            LocalDate to,
            GraphGranularity granularity) {
        Map<LocalDate, SpendingGraphPointDto> pointsByBucket = points.stream()
                .collect(Collectors.toMap(
                        SpendingGraphAggregate::bucket,
                        point -> new SpendingGraphPointDto(point.bucket(), point.totalAmount())));

        LocalDate bucket = alignBucket(from, granularity);
        LocalDate endBucket = alignBucket(to, granularity);
        java.util.ArrayList<SpendingGraphPointDto> filled = new java.util.ArrayList<>();

        while (!bucket.isAfter(endBucket)) {
            SpendingGraphPointDto point = pointsByBucket.get(bucket);
            filled.add(point == null ? new SpendingGraphPointDto(bucket, BigDecimal.ZERO) : point);
            bucket = nextBucket(bucket, granularity);
        }

        return filled;
    }

    private LocalDate alignBucket(LocalDate date, GraphGranularity granularity) {
        return switch (granularity) {
            case DAY -> date;
            case WEEK -> date.minusDays(date.getDayOfWeek().getValue() - 1L);
            case MONTH -> date.withDayOfMonth(1);
        };
    }

    private LocalDate nextBucket(LocalDate bucket, GraphGranularity granularity) {
        return switch (granularity) {
            case DAY -> bucket.plusDays(1);
            case WEEK -> bucket.plusWeeks(1);
            case MONTH -> bucket.plusMonths(1);
        };
    }

    private enum GraphGranularity {
        DAY,
        WEEK,
        MONTH
    }
}
