package com.batu.shared.cursor;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

import tools.jackson.databind.ObjectMapper;

@AutoConfiguration
@ConditionalOnClass({ CursorUtils.class, ObjectMapper.class })
public class CursorAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    CursorUtils cursorUtils(ObjectMapper objectMapper) {
        return new CursorUtils(objectMapper);
    }
}
