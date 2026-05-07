package com.batu.account_service.repository;

import com.batu.account_service.entity.Account;
import com.batu.shared.dto.request.AccountUpsertRequestDto;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

public class AccountRepositoryImpl implements AccountRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Account upsertAccount(AccountUpsertRequestDto request) {
        String sql = """
                insert into accounts (
                    account_id,
                    user_id,
                    connection_id,
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
                    :connectionId,
                    :institutionName,
                    :accountName,
                    :accountType,
                    :accountSubtype,
                    :accountMask,
                    :currentBalance,
                    :availableBalance,
                    :isoCurrencyCode,
                    true,
                    now(),
                    now()
                )
                on conflict (account_id) do update set
                    user_id = excluded.user_id,
                    connection_id = excluded.connection_id,
                    institution_name = excluded.institution_name,
                    account_name = excluded.account_name,
                    account_type = excluded.account_type,
                    account_subtype = excluded.account_subtype,
                    account_mask = excluded.account_mask,
                    current_balance = excluded.current_balance,
                    available_balance = excluded.available_balance,
                    iso_currency_code = excluded.iso_currency_code,
                    is_active = true,
                    updated_at = now()
                returning *
                """;

        return (Account) entityManager.createNativeQuery(sql, Account.class)
                .setParameter("accountId", request.getAccountId())
                .setParameter("userId", request.getUserId())
                .setParameter("connectionId", request.getConnectionId())
                .setParameter("institutionName", request.getInstitutionName())
                .setParameter("accountName", request.getAccountName())
                .setParameter("accountType", request.getAccountType())
                .setParameter("accountSubtype", request.getAccountSubtype())
                .setParameter("accountMask", request.getAccountMask())
                .setParameter("currentBalance", request.getCurrentBalance())
                .setParameter("availableBalance", request.getAvailableBalance())
                .setParameter("isoCurrencyCode", request.getIsoCurrencyCode())
                .getSingleResult();
    }
}
