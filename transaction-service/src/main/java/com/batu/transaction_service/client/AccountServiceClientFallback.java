package com.batu.transaction_service.client;

import java.util.Collections;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import com.batu.transaction_service.dto.AccountInformationRequestDto;
import com.batu.transaction_service.dto.AccountInformationResponseDto;

@Component
public class AccountServiceClientFallback implements AccountServiceClient {

    @Override
    public ResponseEntity<List<AccountInformationResponseDto>> getAccountInformations(
            AccountInformationRequestDto request) {
                return ResponseEntity.ok(Collections.emptyList());
    }

}
