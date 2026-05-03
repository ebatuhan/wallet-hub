package com.batu.account_service.repository;

import com.batu.account_service.entity.Account;
import com.batu.shared.dto.request.AccountUpsertRequestDto;

public interface AccountRepositoryCustom {
    Account upsertAccount(AccountUpsertRequestDto request);
}
