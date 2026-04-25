package com.batu.account_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.beans.factory.annotation.Qualifier;

import tools.jackson.databind.ObjectMapper;

import com.batu.account_service.util.CursorUtils;

@Configuration
public class ObjectMapperConfig {

    @Bean
    @Primary
    public ObjectMapper defaultObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        return mapper;
    }

    @Bean("cursorObjectMapper")
    public ObjectMapper cursorObjectMapper() {
        return new ObjectMapper();
    }

    @Bean
    public CursorUtils cursorUtils(@Qualifier("cursorObjectMapper") ObjectMapper objectMapper) {
        return new CursorUtils(objectMapper);
    }
}
