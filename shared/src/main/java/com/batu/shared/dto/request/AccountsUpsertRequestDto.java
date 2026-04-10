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

public class AccountsUpsertRequestDto {

    private List<AccountRequestDto> accounts = new ArrayList<>();
}
