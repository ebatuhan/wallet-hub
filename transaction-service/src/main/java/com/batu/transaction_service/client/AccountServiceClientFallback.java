package com.batu.transaction_service.client;

import java.util.Collections;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import com.batu.shared.dto.AccountNameRequestDto;
import com.batu.shared.dto.AccountNameResponseDto;

@Component
public class AccountServiceClientFallback implements AccountServiceClient {

    @Override
    public ResponseEntity<List<AccountNameResponseDto>> getAccountNames(
            AccountNameRequestDto request) {
                return ResponseEntity.ok(Collections.emptyList());
    }

}
