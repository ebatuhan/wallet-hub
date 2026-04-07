package com.batu.shared.dto.response;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AccountNameResponseDto {

    private UUID accountId;
    private String accountName;
}
