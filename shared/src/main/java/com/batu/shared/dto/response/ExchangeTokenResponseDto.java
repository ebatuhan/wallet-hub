package com.batu.shared.dto.response;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ExchangeTokenResponseDto {

    private UUID connectionId;
    private String institutionId;
    private String institutionName;
}
