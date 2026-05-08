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

public interface AccountRepository extends JpaRepository<Account, UUID>, JpaSpecificationExecutor<Account>, AccountRepositoryCustom {
    Optional<Account> findByAccountIdAndUserIdAndIsActiveTrue(UUID accountId, UUID userId);

    Optional<Account> findByAccountIdAndUserId(UUID accountId, UUID userId);

    List<Account> findByConnectionIdAndIsActiveTrue(UUID connectionId);

    List<Account> findByConnectionIdAndIsActiveTrueOrderByCreatedAtDesc(UUID connectionId);

    @Query("""
            select account.accountId as accountId,
                   account.userId as userId,
                   account.connectionId as connectionId
            from Account account
            where account.connectionId = :connectionId
              and account.isActive = true
            """)
    List<AccountRemovalProjection> findActiveAccountRemovalsByConnectionId(@Param("connectionId") UUID connectionId);

    @Modifying
    @Query("""
            update Account account
            set account.isActive = false
            where account.connectionId = :connectionId
              and account.isActive = true
            """)
    int deactivateActiveAccountsByConnectionId(@Param("connectionId") UUID connectionId);

    List<Account> findAllByAccountIdIn(Collection<UUID> accountIds);

    long countByUserIdAndIsActiveTrue(UUID userId);

    @Query("""
            select new com.batu.shared.dto.response.AccountNameResponseDto(
                account.accountId,
                account.accountName
            )
            from Account account
            where account.accountId in :accountIds
            """)
    List<AccountNameResponseDto> findByAccountIdIn(@Param("accountIds") Set<UUID> accountIds);

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

    interface AccountCurrencyTotalProjection {
        String getIsoCurrencyCode();

        BigDecimal getCurrentBalanceTotal();

        BigDecimal getAvailableBalanceTotal();
    }

    interface AccountRemovalProjection {
        UUID getAccountId();

        UUID getUserId();

        UUID getConnectionId();
    }
}
