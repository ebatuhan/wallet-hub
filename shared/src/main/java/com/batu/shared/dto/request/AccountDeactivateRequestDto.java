package com.batu.shared.dto.request;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AccountDeactivateRequestDto {

    private UUID accountId;
    private long syncVersion;
}
