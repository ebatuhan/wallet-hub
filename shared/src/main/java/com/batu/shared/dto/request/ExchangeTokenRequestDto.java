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
public class ExchangeTokenRequestDto {

    private String publicToken;
    private List<String> accountIds = new ArrayList<>();
    private String institutionId;
    private String institutionName;

    public ExchangeTokenRequestDto(String publicToken, String institutionId, String institutionName) {
        this.publicToken = publicToken;
        this.institutionId = institutionId;
        this.institutionName = institutionName;
    }
}
