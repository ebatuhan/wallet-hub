package com.batu.transaction_service;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.batu.shared.dto.request.PrimaryCategoryIdsRequestDto;
import com.batu.shared.dto.response.TransactionPrimaryCategoryDto;
import com.batu.transaction_service.service.PrimaryCategoryService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/transactions/categories/primary")
public class TransactionCategoryController {

    private final PrimaryCategoryService primaryCategoryService;

    public TransactionCategoryController(PrimaryCategoryService primaryCategoryService) {
        this.primaryCategoryService = primaryCategoryService;
    }

    @GetMapping
    public ResponseEntity<List<TransactionPrimaryCategoryDto>> getAllPrimaryCategories() {
        return ResponseEntity.ok(primaryCategoryService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TransactionPrimaryCategoryDto> getPrimaryCategoryById(@PathVariable UUID id) {
        var category = primaryCategoryService.getById(id);
        return ResponseEntity.ok(new TransactionPrimaryCategoryDto(
                category.getTransactionPrimaryCategoryId(),
                category.getCategoryCode(),
                category.getDisplayName(),
                category.getIconUrl()));
    }

    @PostMapping("/by-ids")
    public ResponseEntity<List<TransactionPrimaryCategoryDto>> getPrimaryCategoriesByIds(
            @Valid @RequestBody PrimaryCategoryIdsRequestDto request) {
        return ResponseEntity.ok(primaryCategoryService.getByIds(request.getIds()));
    }
}
