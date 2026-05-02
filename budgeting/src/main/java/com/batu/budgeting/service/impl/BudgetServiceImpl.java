package com.batu.budgeting.service.impl;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.batu.budgeting.client.TransactionCategoryClient;
import com.batu.budgeting.dto.BudgetResponse;
import com.batu.budgeting.dto.CreateBudgetRequest;
import com.batu.budgeting.entity.Budget;
import com.batu.budgeting.exception.BudgetConflictException;
import com.batu.budgeting.exception.ResourceNotFoundException;
import com.batu.budgeting.mapper.BudgetMapper;
import com.batu.budgeting.repository.BudgetRepository;
import com.batu.budgeting.service.BudgetService;
import com.batu.budgeting.service.input.ApplyTransactionInput;
import com.batu.shared.dto.request.PrimaryCategoryIdsRequestDto;
import com.batu.shared.dto.response.TransactionPrimaryCategoryDto;

@Service
public class BudgetServiceImpl implements BudgetService {

    private final BudgetRepository budgetRepository;
    private final BudgetMapper budgetMapper;
    private final TransactionCategoryClient transactionCategoryClient;

    public BudgetServiceImpl(BudgetRepository budgetRepository,
            BudgetMapper budgetMapper,
            TransactionCategoryClient transactionCategoryClient) {
        this.budgetRepository = budgetRepository;
        this.budgetMapper = budgetMapper;
        this.transactionCategoryClient = transactionCategoryClient;
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
        TransactionPrimaryCategoryDto category = transactionCategoryClient
                .getPrimaryCategoryById(savedBudget.getCategoryId())
                .getBody();
        return budgetMapper.toResponse(savedBudget, category);
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
        TransactionPrimaryCategoryDto category = transactionCategoryClient
                .getPrimaryCategoryById(savedBudget.getCategoryId())
                .getBody();
        return budgetMapper.toResponse(savedBudget, category);
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
        Map<UUID, TransactionPrimaryCategoryDto> categoriesById = loadCategoryMetadataByIds(
                budgets.stream().map(Budget::getCategoryId).collect(java.util.stream.Collectors.toSet()));

        return budgets.stream()
                .map(budget -> budgetMapper.toResponse(budget, categoriesById.get(budget.getCategoryId())))
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
    public void applyTransaction(ApplyTransactionInput input) {
        if (!input.active() || Boolean.TRUE.equals(input.pending())) {
            return;
        }

        if (input.primaryCategoryId() == null || input.amount() == null || input.date() == null
                || input.isoCurrencyCode() == null) {
            return;
        }

        if (input.amount().signum() >= 0) {
            return;
        }

        List<Budget> candidates = budgetRepository.findCandidates(
                input.userId(),
                input.primaryCategoryId(),
                input.isoCurrencyCode(),
                input.date()).stream()
                .filter(budget -> !budget.getPeriod().computeEnd(budget.getPeriodStart()).isBefore(input.date()))
                .toList();

        if (candidates.isEmpty()) {
            return;
        }

        Budget budget = candidates.getFirst();
        BigDecimal spentAmount = input.amount().abs();

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

    private Map<UUID, TransactionPrimaryCategoryDto> loadCategoryMetadataByIds(Set<UUID> categoryIds) {
        if (categoryIds.isEmpty()) {
            return Map.of();
        }

        List<TransactionPrimaryCategoryDto> categories = transactionCategoryClient
                .getPrimaryCategoriesByIds(new PrimaryCategoryIdsRequestDto(categoryIds))
                .getBody();

        if (categories == null || categories.isEmpty()) {
            return Map.of();
        }

        Map<UUID, TransactionPrimaryCategoryDto> categoriesById = new HashMap<>();
        for (TransactionPrimaryCategoryDto category : categories) {
            categoriesById.put(category.getTransactionPrimaryCategoryId(), category);
        }
        return categoriesById;
    }

}
