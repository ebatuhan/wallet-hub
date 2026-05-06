package com.batu.transaction_service.service.impl;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.batu.shared.dto.response.TransactionPrimaryCategoryDto;
import com.batu.transaction_service.entity.TransactionPrimaryCategory;
import com.batu.transaction_service.repository.PrimaryCategoryRepository;
import com.batu.transaction_service.service.PrimaryCategoryService;

@Service
public class PrimaryCategoryServiceImpl implements PrimaryCategoryService {
    private final PrimaryCategoryRepository primaryCategoryRepository;

    public PrimaryCategoryServiceImpl(PrimaryCategoryRepository primaryCategoryRepository) {
        this.primaryCategoryRepository = primaryCategoryRepository;
    }

    @Override
    public TransactionPrimaryCategory getByCategoryCode(String categoryCode) {
        return primaryCategoryRepository.findByCategoryCode(categoryCode)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Primary category with code " + categoryCode + " not found"));
    }

    @Override
    public TransactionPrimaryCategory getById(UUID primaryCategoryId) {
        return primaryCategoryRepository.findById(primaryCategoryId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Primary category with id " + primaryCategoryId + " not found"));
    }

    @Override
    public List<TransactionPrimaryCategoryDto> getAll() {
        return primaryCategoryRepository.findAll().stream()
                .map(category -> new TransactionPrimaryCategoryDto(
                        category.getTransactionPrimaryCategoryId(),
                        category.getCategoryCode(),
                        category.getDisplayName(),
                        category.getIconUrl()))
                .toList();
    }

    @Override
    public List<TransactionPrimaryCategoryDto> getByIds(Set<UUID> primaryCategoryIds) {
        return primaryCategoryRepository.findByTransactionPrimaryCategoryIdIn(primaryCategoryIds).stream()
                .map(category -> new TransactionPrimaryCategoryDto(
                        category.getTransactionPrimaryCategoryId(),
                        category.getCategoryCode(),
                        category.getDisplayName(),
                        category.getIconUrl()))
                .toList();
    }
}
