package com.batu.shared.dto.request;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PrimaryCategoryIdsRequestDto {

    private Set<UUID> ids = new HashSet<>();
}
