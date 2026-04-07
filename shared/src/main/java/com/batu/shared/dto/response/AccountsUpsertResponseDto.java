package com.batu.shared.dto.response;

import java.util.Map;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AccountsUpsertResponseDto {

    private Map<String, UUID> insertedAccountsMap;
}
