package com.batu.account_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.beans.factory.annotation.Qualifier;

import com.batu.shared.util.CursorUtils;

import tools.jackson.databind.ObjectMapper;

@Configuration
public class ObjectMapperConfig {

    @Bean
    @Primary
    public ObjectMapper defaultObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        // Your DTO mapping configuration here
        // mapper.registerModule(new JavaTimeModule());
        // mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        // etc.
        return mapper;
    }

    @Bean("cursorObjectMapper") //TODO WUN DA FUK
    public ObjectMapper cursorObjectMapper() {
        // Simple ObjectMapper for cursor encoding/decoding
        ObjectMapper mapper = new ObjectMapper();
        // Minimal configuration for cursor operations only
        return mapper;
    }

    @Bean
    public CursorUtils cursorUtils(@Qualifier("cursorObjectMapper") ObjectMapper objectMapper) {
        return new CursorUtils(objectMapper);
    }
}
