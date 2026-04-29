package com.batu.shared.dto.request;

import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ConnectionExchangeRequestDto {

    private String provider;
    private String publicToken;
    private List<String> accountIds = new ArrayList<>();
    private List<ConnectionAccountMetadataDto> accounts = new ArrayList<>();
    private String institutionId;
    private String institutionName;
}
