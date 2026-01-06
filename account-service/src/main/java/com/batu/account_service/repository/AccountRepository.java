package com.batu.account_service.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.batu.account_service.dto.AccountResponseDto;
import com.batu.account_service.entity.Account;
import com.batu.shared.dto.AccountNameResponseDto;

public interface AccountRepository extends JpaRepository<Account, UUID>, JpaSpecificationExecutor<Account> {
    Optional<AccountResponseDto> findByAccountIdAndUserIdAndIsActiveTrue(UUID accountId, UUID userId);

    List<AccountNameResponseDto> findByAccountIdIn(Set<UUID> accountIds);

    @Modifying
    @Query(value = """
                INSERT INTO accounts (
                    connection_id, user_id, external_id, account_name, account_type,
                    account_subtype, account_mask, current_balance, available_balance, iso_currency_code, is_active
                )
                VALUES (:connectionId, :userId, :externalId, :accountName, :accountType, :accountSubtype,
                        :accountMask, :currentBalance, :availableBalance, :isoCurrencyCode, :isActive)
                ON CONFLICT (external_id)
                DO UPDATE SET
                    account_name = EXCLUDED.account_name,
                    account_type = EXCLUDED.account_type,
                    account_subtype = EXCLUDED.account_subtype,
                    account_mask = EXCLUDED.account_mask,
                    current_balance = EXCLUDED.current_balance,
                    available_balance = EXCLUDED.available_balance,
                    iso_currency_code = EXCLUDED.iso_currency_code,
                    is_active = EXCLUDED.is_active,
                    updated_at = now()
                RETURNING account_id
            """, nativeQuery = true)
    List<UUID> upsertAccount(
            @Param("connectionId") UUID connectionId,
            @Param("userId") UUID userId,
            @Param("externalId") String externalId,
            @Param("accountName") String accountName,
            @Param("accountType") String accountType,
            @Param("accountSubtype") String accountSubtype,
            @Param("accountMask") String accountMask,
            @Param("currentBalance") BigDecimal currentBalance,
            @Param("availableBalance") BigDecimal availableBalance,
            @Param("isoCurrencyCode") String isoCurrencyCode,
            @Param("isActive") Boolean isActive);

}
