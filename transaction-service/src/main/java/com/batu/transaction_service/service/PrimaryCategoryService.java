package com.batu.transaction_service.service;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.batu.transaction_service.entity.TransactionPrimaryCategory;

@Service
public interface PrimaryCategoryService {    
    TransactionPrimaryCategory getByCategoryCode(String categoryCode);
    TransactionPrimaryCategory getById(UUID primaryCategoryId);
}
