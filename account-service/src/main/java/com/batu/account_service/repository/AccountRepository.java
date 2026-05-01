package com.batu.account_service.repository;

import java.math.BigDecimal;
import java.util.Collection;
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

    List<Account> findAllByAccountIdIn(Collection<UUID> accountIds);

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
            insert into accounts (account_id, user_id, institution_name, account_name, account_type, account_subtype,
                                  account_mask, current_balance, available_balance, iso_currency_code, is_active,
                                  sync_version, created_at, updated_at)
            values (:accountId, :userId, :institutionName, :accountName, :accountType, :accountSubtype,
                    :accountMask, :currentBalance, :availableBalance, :isoCurrencyCode, :active,
                    :syncVersion, current_timestamp, current_timestamp)
            on conflict (account_id) do update
            set user_id = excluded.user_id,
                institution_name = excluded.institution_name,
                account_name = excluded.account_name,
                account_type = excluded.account_type,
                account_subtype = excluded.account_subtype,
                account_mask = excluded.account_mask,
                current_balance = excluded.current_balance,
                available_balance = excluded.available_balance,
                iso_currency_code = excluded.iso_currency_code,
                is_active = excluded.is_active,
                sync_version = excluded.sync_version,
                updated_at = current_timestamp
            where accounts.sync_version <= excluded.sync_version
            """, nativeQuery = true)
    int upsertFromSync(@Param("accountId") UUID accountId,
            @Param("userId") UUID userId,
            @Param("institutionName") String institutionName,
            @Param("accountName") String accountName,
            @Param("accountType") String accountType,
            @Param("accountSubtype") String accountSubtype,
            @Param("accountMask") String accountMask,
            @Param("currentBalance") BigDecimal currentBalance,
            @Param("availableBalance") BigDecimal availableBalance,
            @Param("isoCurrencyCode") String isoCurrencyCode,
            @Param("active") boolean active,
            @Param("syncVersion") long syncVersion);

    @Modifying
    @Query(value = """
            update accounts
            set is_active = false,
                sync_version = :syncVersion,
                updated_at = current_timestamp
            where account_id = :accountId
              and sync_version <= :syncVersion
            """, nativeQuery = true)
    int deactivateFromSync(@Param("accountId") UUID accountId, @Param("syncVersion") long syncVersion);

    interface AccountCurrencyTotalProjection {
        String getIsoCurrencyCode();

        BigDecimal getCurrentBalanceTotal();

        BigDecimal getAvailableBalanceTotal();
    }
}
