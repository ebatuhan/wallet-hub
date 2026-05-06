package com.batu.budgeting.service.impl;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.ScrollPosition;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Window;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.batu.budgeting.dto.BudgetResponse;
import com.batu.budgeting.dto.CreateBudgetRequest;
import com.batu.budgeting.entity.Budget;
import com.batu.budgeting.enums.BudgetSortField;
import com.batu.budgeting.mapper.BudgetMapper;
import com.batu.budgeting.repository.BudgetRepository;
import com.batu.budgeting.service.BudgetService;
import com.batu.shared.cursor.CursorUtils;
import com.batu.shared.dto.response.CursorResponse;
import com.batu.shared.messaging.event.TransactionRecorded;

@Service
public class BudgetServiceImpl implements BudgetService {

    private final BudgetRepository budgetRepository;
    private final BudgetMapper budgetMapper;
    private final CursorUtils cursorUtils;

    public BudgetServiceImpl(BudgetRepository budgetRepository,
            BudgetMapper budgetMapper,
            CursorUtils cursorUtils) {
        this.budgetRepository = budgetRepository;
        this.budgetMapper = budgetMapper;
        this.cursorUtils = cursorUtils;
    }

    @Override
    @Transactional
    public BudgetResponse createBudget(CreateBudgetRequest request, Jwt principal) {
        return createBudget(request, UUID.fromString(principal.getSubject()));
    }

    @Override
    @Transactional
    public BudgetResponse createBudget(CreateBudgetRequest request, UUID userId) {
        validateNoOverlap(userId, request);

        Budget budget = new Budget(
                userId,
                request.categoryId(),
                request.limitAmount(),
                request.isoCurrencyCode(),
                request.period(),
                request.periodStart());

        Budget savedBudget = budgetRepository.save(budget);
        return budgetMapper.toResponse(savedBudget);
    }

    @Override
    @Transactional
    public BudgetResponse updateBudget(UUID budgetId, CreateBudgetRequest request, Jwt principal) {
        return updateBudget(budgetId, request, UUID.fromString(principal.getSubject()));
    }

    @Override
    @Transactional
    public BudgetResponse updateBudget(UUID budgetId, CreateBudgetRequest request, UUID userId) {
        Budget budget = budgetRepository.findByIdAndUserId(budgetId, userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Budget with id " + budgetId + " not found"));

        validateNoOverlap(userId, request, budgetId);

        budget.setCategoryId(request.categoryId());
        budget.setLimitAmount(request.limitAmount());
        budget.setIsoCurrencyCode(request.isoCurrencyCode());
        budget.setPeriod(request.period());
        budget.setPeriodStart(request.periodStart());
        budget.setUpdatedAt(Instant.now());

        Budget savedBudget = budgetRepository.save(budget);
        return budgetMapper.toResponse(savedBudget);
    }

    @Override
    @Transactional(readOnly = true)
    public CursorResponse<BudgetResponse> getBudgets(Jwt principal, String cursor, int limit, BudgetSortField sortBy,
            Sort.Direction direction) {
        return getBudgets(UUID.fromString(principal.getSubject()), cursor, limit, sortBy, direction);
    }

    @Override
    @Transactional(readOnly = true)
    public CursorResponse<BudgetResponse> getBudgets(UUID userId, String cursor, int limit, BudgetSortField sortBy,
            Sort.Direction direction) {
        Sort sort = sortBy == null
                ? Sort.by(direction, "createdAt").and(Sort.by("id"))
                : Sort.by(direction, sortBy.getFieldName()).and(Sort.by("id"));

        ScrollPosition scrollPosition = cursor != null
                ? cursorUtils.decode(cursor)
                : ScrollPosition.keyset();

        Specification<Budget> spec = (root, query, criteriaBuilder) -> criteriaBuilder.and(
                criteriaBuilder.equal(root.get("userId"), userId),
                criteriaBuilder.isTrue(root.get("active")));

        Window<Budget> budgets = budgetRepository.findBy(spec, query -> query
                .sortBy(sort)
                .limit(limit)
                .scroll(scrollPosition));

        String nextCursor = budgets.hasNext()
                ? cursorUtils.encode(budgets.positionAt(budgets.size() - 1))
                : null;

        List<BudgetResponse> data = budgets.getContent().stream()
                .map(budgetMapper::toResponse)
                .toList();

        return new CursorResponse<>(data, budgets.hasNext(), nextCursor);
    }

    @Override
    @Transactional
    public void deactivateBudget(UUID budgetId, Jwt principal) {
        deactivateBudget(budgetId, UUID.fromString(principal.getSubject()));
    }

    @Override
    @Transactional
    public void deactivateBudget(UUID budgetId, UUID userId) {
        Budget budget = budgetRepository.findByIdAndUserId(budgetId, userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Budget with id " + budgetId + " not found"));

        budget.setActive(false);
        budget.setUpdatedAt(Instant.now());
        budgetRepository.save(budget);
    }

    @Override
    @Transactional
    public void applyTransaction(TransactionRecorded transaction) {
        if (!transaction.isActive() || Boolean.TRUE.equals(transaction.getPending())) {
            return;
        }

        if (transaction.getPrimaryCategoryId() == null || transaction.getAmount() == null || transaction.getDate() == null
                || transaction.getIsoCurrencyCode() == null) {
            return;
        }

        if (transaction.getAmount().signum() >= 0) {
            return;
        }

        List<Budget> candidates = budgetRepository.findCandidates(
                transaction.getUserId(),
                transaction.getPrimaryCategoryId(),
                transaction.getIsoCurrencyCode(),
                transaction.getDate()).stream()
                .filter(budget -> !budget.getPeriod().computeEnd(budget.getPeriodStart()).isBefore(transaction.getDate()))
                .toList();

        if (candidates.isEmpty()) {
            return;
        }

        Budget budget = candidates.getFirst();
        BigDecimal spentAmount = transaction.getAmount().abs();

        budget.setSpentAmount(budget.getSpentAmount().add(spentAmount));
        budget.setUpdatedAt(Instant.now());
        budgetRepository.save(budget);
    }

    private void validateNoOverlap(UUID userId, CreateBudgetRequest request) {
        validateNoOverlap(userId, request, null);
    }

    private void validateNoOverlap(UUID userId, CreateBudgetRequest request, UUID ignoredBudgetId) {
        List<Budget> budgets = budgetRepository.findByUserIdAndCategoryIdAndIsoCurrencyCodeAndActiveTrue(
                userId,
                request.categoryId(),
                request.isoCurrencyCode());

        var newEnd = request.period().computeEnd(request.periodStart());

        for (Budget existing : budgets) {
            if (ignoredBudgetId != null && ignoredBudgetId.equals(existing.getId())) {
                continue;
            }

            var existingEnd = existing.getPeriod().computeEnd(existing.getPeriodStart());
            boolean overlaps = !existingEnd.isBefore(request.periodStart()) && !newEnd.isBefore(existing.getPeriodStart());

            if (overlaps) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Overlapping active budget exists for this category and currency");
            }
        }
    }

}
