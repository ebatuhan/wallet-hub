package com.batu.insights_service.service.impl;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import com.batu.insights_service.entity.TransactionInsightRow;
import com.batu.insights_service.exception.InvalidDateRangeException;
import com.batu.insights_service.repository.TransactionInsightsRepository;
import com.batu.insights_service.service.TransactionInsightsService;
import com.batu.shared.dto.response.IncomeSummaryResponseDto;
import com.batu.shared.dto.response.SpendingGraphPointDto;
import com.batu.shared.dto.response.SpendingGraphResponseDto;
import com.batu.shared.dto.response.SpendingPerCategoryByAccountDto;
import com.batu.shared.dto.response.SpendingPerCategoryByAccountResponseDto;
import com.batu.shared.dto.response.SpendingPerCategoryDto;
import com.batu.shared.dto.response.SpendingPerCategoryResponseDto;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TransactionInsightsServiceImpl implements TransactionInsightsService {

    private final TransactionInsightsRepository transactionInsightsRepository;

    @Override
    public SpendingPerCategoryResponseDto getSpendingByCategory(Date from, Date to, Jwt principal) {
        validateDateRange(from, to);
        UUID userId = UUID.fromString(principal.getSubject());

        List<SpendingPerCategoryDto> categories = transactionInsightsRepository.findByInterval(from, to, userId);
        return new SpendingPerCategoryResponseDto(userId, totalSpent(categories), categories);
    }

    @Override
    public SpendingGraphResponseDto getSpendingGraph(Date from, Date to, Jwt principal) {
        validateDateRange(from, to);
        UUID userId = UUID.fromString(principal.getSubject());
        LocalDate fromDate = from.toLocalDate();
        LocalDate toDate = to.toLocalDate();
        GraphGranularity granularity = determineGranularity(fromDate, toDate);

        List<SpendingGraphPointDto> points = fillMissingPoints(
                transactionInsightsRepository.findSpendingGraphByInterval(from, to, userId, bucketExpression(granularity)),
                fromDate,
                toDate,
                granularity);

        return new SpendingGraphResponseDto(userId, null, granularity.name(), fromDate, toDate, points);
    }

    @Override
    public SpendingGraphResponseDto getSpendingGraphByAccount(Date from, Date to, UUID accountId, Jwt principal) {
        validateDateRange(from, to);
        UUID userId = UUID.fromString(principal.getSubject());
        LocalDate fromDate = from.toLocalDate();
        LocalDate toDate = to.toLocalDate();
        GraphGranularity granularity = determineGranularity(fromDate, toDate);

        List<SpendingGraphPointDto> points = fillMissingPoints(
                transactionInsightsRepository.findSpendingGraphByIntervalAndAccount(
                        from,
                        to,
                        userId,
                        accountId,
                        bucketExpression(granularity)),
                fromDate,
                toDate,
                granularity);

        return new SpendingGraphResponseDto(userId, accountId, granularity.name(), fromDate, toDate, points);
    }

    @Override
    public SpendingPerCategoryByAccountResponseDto getSpendingPerCategoryByAccount(Date from, Date to, UUID accountId,
            Jwt principal) {

        validateDateRange(from, to);
        UUID userId = UUID.fromString(principal.getSubject());
        List<SpendingPerCategoryByAccountDto> categories = transactionInsightsRepository.findByIntervalAndAccount(from, to, userId, accountId);
        return new SpendingPerCategoryByAccountResponseDto(userId, accountId, totalSpentByAccount(categories), categories);

    }

    @Override
    public IncomeSummaryResponseDto getIncome(Date from, Date to, Jwt principal) {
        validateDateRange(from, to);
        UUID userId = UUID.fromString(principal.getSubject());
        return new IncomeSummaryResponseDto(userId, transactionInsightsRepository.findIncomeByInterval(from, to, userId));
    }

    @Override
    public void save(TransactionInsightRow transactionInsightRow) {
        transactionInsightsRepository.save(transactionInsightRow);
    }

    private void validateDateRange(Date from, Date to) {
        if (from.after(to)) {
            throw new InvalidDateRangeException("'from' date must be before or equal to 'to' date");
        }
    }

    private BigDecimal totalSpent(List<SpendingPerCategoryDto> categories) {
        return categories.stream()
                .map(SpendingPerCategoryDto::totalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal totalSpentByAccount(List<SpendingPerCategoryByAccountDto> categories) {
        return categories.stream()
                .map(SpendingPerCategoryByAccountDto::totalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
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

    private List<SpendingGraphPointDto> fillMissingPoints(List<SpendingGraphPointDto> points,
            LocalDate from,
            LocalDate to,
            GraphGranularity granularity) {
        Map<LocalDate, SpendingGraphPointDto> pointsByBucket = points.stream()
                .collect(Collectors.toMap(SpendingGraphPointDto::bucket, Function.identity()));

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
