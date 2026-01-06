package com.batu.account_service.repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import com.batu.account_service.dto.AccountResponseDto;
import com.batu.account_service.entity.Account;
import com.batu.shared.dto.AccountNameResponseDto;

public interface AccountRepository extends JpaRepository<Account, UUID>, JpaSpecificationExecutor<Account>{
    Optional<AccountResponseDto> findByAccountIdAndUserIdAndIsActiveTrue(UUID accountId, UUID userId);

    List<AccountNameResponseDto> findByAccountIdIn(Set<UUID> accountIds);

}
