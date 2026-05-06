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
public class PrimaryCategoryIdsRequestDto {

    @NotEmpty(message = "At least one category id is required")
    private Set<@NotNull(message = "Category id is required") UUID> ids = new HashSet<>();
}
