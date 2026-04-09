package com.batu.shared.messaging.saga;

import java.util.List;
import java.util.UUID;

import com.batu.shared.dto.request.AccountRequestDto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
@Builder
public class PersistAccountsCommand {
    private UUID sagaId;
    private List<AccountRequestDto> accountsPayload;
}
