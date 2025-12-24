package com.batu.plaid_adapter_service.mapper;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;


import com.batu.plaid_adapter_service.entity.Connection;

@Mapper(componentModel = "spring")
public interface ConnectionMapper {
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateConnectionFromSource(Connection source, @MappingTarget Connection target);
}
