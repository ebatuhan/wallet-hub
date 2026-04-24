package com.batu.transaction_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.batu.transaction_service.util.CursorUtils;

import tools.jackson.databind.ObjectMapper;

@Configuration
public class ObjectMapperConfig {

    @Bean
    public CursorUtils cursorUtils(ObjectMapper objectMapper) {
        return new CursorUtils(objectMapper);
    }
}
