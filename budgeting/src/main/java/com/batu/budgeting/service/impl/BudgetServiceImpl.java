package com.batu.budgeting.service.impl;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.batu.budgeting.dto.BudgetResponse;
import com.batu.budgeting.dto.CreateBudgetRequest;
import com.batu.budgeting.entity.Budget;
import com.batu.budgeting.exception.BudgetConflictException;
import com.batu.budgeting.exception.ResourceNotFoundException;
import com.batu.budgeting.mapper.BudgetMapper;
import com.batu.budgeting.repository.BudgetRepository;
import com.batu.budgeting.service.BudgetService;
import com.batu.shared.messaging.event.TransactionRecorded;

@Service
public class BudgetServiceImpl implements BudgetService {

    private final BudgetRepository budgetRepository;
    private final BudgetMapper budgetMapper;

    public BudgetServiceImpl(BudgetRepository budgetRepository,
            BudgetMapper budgetMapper) {
        this.budgetRepository = budgetRepository;
        this.budgetMapper = budgetMapper;
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
                .orElseThrow(() -> new ResourceNotFoundException("Budget with id " + budgetId + " not found"));

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
    public List<BudgetResponse> getBudgets(Jwt principal) {
        return getBudgets(UUID.fromString(principal.getSubject()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<BudgetResponse> getBudgets(UUID userId) {
        List<Budget> budgets = budgetRepository.findByUserIdAndActiveTrue(userId);
        return budgets.stream()
                .map(budgetMapper::toResponse)
                .toList();
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
                .orElseThrow(() -> new ResourceNotFoundException("Budget with id " + budgetId + " not found"));

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
                throw new BudgetConflictException("Overlapping active budget exists for this category and currency");
            }
        }
    }

}
