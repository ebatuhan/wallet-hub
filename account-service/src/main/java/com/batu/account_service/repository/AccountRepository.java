package com.batu.account_service.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.batu.account_service.dto.AccountResponseDto;
import com.batu.account_service.entity.Account;

public interface AccountRepository extends JpaRepository<Account, UUID>, JpaSpecificationExecutor<Account>{
    Optional<AccountResponseDto> findByAccountIdAndUserIdAndIsActiveTrue(UUID accountId, UUID userId);
}
