package com.batu.shared.dto.request;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AccountNameRequestDto {

    @NotEmpty(message = "At least one account id is required")
    private Set<@NotNull(message = "Account id is required") UUID> accountIds = new HashSet<>();
}
