package com.batu.transaction_service.controller;

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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/transactions/categories/primary")
@Tag(name = "Transaction Categories", description = "Primary transaction category lookup endpoints.")
@SecurityRequirement(name = "bearerAuth")
public class TransactionCategoryController {

    private final PrimaryCategoryService primaryCategoryService;

    public TransactionCategoryController(PrimaryCategoryService primaryCategoryService) {
        this.primaryCategoryService = primaryCategoryService;
    }

    @GetMapping
    @Operation(summary = "List primary categories", description = "Returns all primary transaction categories.")
    public ResponseEntity<List<TransactionPrimaryCategoryDto>> getAllPrimaryCategories() {
        return ResponseEntity.ok(primaryCategoryService.getAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get primary category", description = "Returns one primary transaction category by ID.")
    public ResponseEntity<TransactionPrimaryCategoryDto> getPrimaryCategoryById(@PathVariable UUID id) {
        var category = primaryCategoryService.getById(id);
        return ResponseEntity.ok(new TransactionPrimaryCategoryDto(
                category.getTransactionPrimaryCategoryId(),
                category.getCategoryCode(),
                category.getDisplayName(),
                category.getIconUrl()));
    }

    @PostMapping("/by-ids")
    @Operation(summary = "Get primary categories by IDs", description = "Returns primary categories for the supplied IDs.")
    public ResponseEntity<List<TransactionPrimaryCategoryDto>> getPrimaryCategoriesByIds(
            @Valid @RequestBody PrimaryCategoryIdsRequestDto request) {
        return ResponseEntity.ok(primaryCategoryService.getByIds(request.getIds()));
    }
}
