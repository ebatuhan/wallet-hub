package com.batu.shared.messaging.saga;

import java.util.List;
import java.util.UUID;

import com.batu.shared.dto.request.AccountRequestDto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InitialAccountsFetchedEvent {
    private UUID sagaId;
    private UUID connectionId;
    private List<AccountRequestDto> accounts;
}
