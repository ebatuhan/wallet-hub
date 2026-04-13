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

import com.batu.account_service.entity.Account;
import com.batu.shared.dto.response.AccountNameResponseDto;
import com.batu.shared.dto.response.AccountResponseDto;

public interface AccountRepository extends JpaRepository<Account, UUID>, JpaSpecificationExecutor<Account> {
    Optional<AccountResponseDto> findByAccountIdAndUserIdAndIsActiveTrue(UUID accountId, UUID userId);

    Optional<Account> findByAccountIdAndUserId(UUID accountId, UUID userId);

    long countByUserIdAndIsActiveTrue(UUID userId);

    List<AccountNameResponseDto> findByAccountIdIn(Set<UUID> accountIds);

    @Query("""
            select account.isoCurrencyCode as isoCurrencyCode,
                   sum(account.currentBalance) as currentBalanceTotal,
                   sum(account.availableBalance) as availableBalanceTotal
            from Account account
            where account.userId = :userId
              and account.isActive = true
            group by account.isoCurrencyCode
            """)
    List<AccountCurrencyTotalProjection> summarizeActiveBalancesByCurrency(@Param("userId") UUID userId);

    @Modifying
    @Query(value = """
            insert into accounts (
                account_id,
                user_id,
                institution_name,
                account_name,
                account_type,
                account_subtype,
                account_mask,
                current_balance,
                available_balance,
                iso_currency_code,
                is_active,
                created_at,
                updated_at
            ) values (
                :accountId,
                :userId,
                :institutionName,
                :accountName,
                :accountType,
                :accountSubtype,
                :accountMask,
                :currentBalance,
                :availableBalance,
                :isoCurrencyCode,
                :isActive,
                current_timestamp,
                current_timestamp
            )
            """, nativeQuery = true)
    int insertSyncedAccount(
            @Param("accountId") UUID accountId,
            @Param("userId") UUID userId,
            @Param("institutionName") String institutionName,
            @Param("accountName") String accountName,
            @Param("accountType") String accountType,
            @Param("accountSubtype") String accountSubtype,
            @Param("accountMask") String accountMask,
            @Param("currentBalance") BigDecimal currentBalance,
            @Param("availableBalance") BigDecimal availableBalance,
            @Param("isoCurrencyCode") String isoCurrencyCode,
            @Param("isActive") boolean isActive);

    @Modifying
    @Query("""
            update Account account
            set account.userId = :userId,
                account.institutionName = :institutionName,
                account.accountName = :accountName,
                account.accountType = :accountType,
                account.accountSubtype = :accountSubtype,
                account.accountMask = :accountMask,
                account.currentBalance = :currentBalance,
                account.availableBalance = :availableBalance,
                account.isoCurrencyCode = :isoCurrencyCode,
                account.isActive = :isActive
            where account.accountId = :accountId
            """)
    int updateSyncedAccount(
            @Param("accountId") UUID accountId,
            @Param("userId") UUID userId,
            @Param("institutionName") String institutionName,
            @Param("accountName") String accountName,
            @Param("accountType") String accountType,
            @Param("accountSubtype") String accountSubtype,
            @Param("accountMask") String accountMask,
            @Param("currentBalance") BigDecimal currentBalance,
            @Param("availableBalance") BigDecimal availableBalance,
            @Param("isoCurrencyCode") String isoCurrencyCode,
            @Param("isActive") boolean isActive);

    @Modifying
    @Query("""
            update Account account
            set account.isActive = false
            where account.accountId = :accountId
              and account.userId = :userId
            """)
    int deactivateSyncedAccount(
            @Param("accountId") UUID accountId,
            @Param("userId") UUID userId);

    interface AccountCurrencyTotalProjection {
        String getIsoCurrencyCode();

        BigDecimal getCurrentBalanceTotal();

        BigDecimal getAvailableBalanceTotal();
    }
}
