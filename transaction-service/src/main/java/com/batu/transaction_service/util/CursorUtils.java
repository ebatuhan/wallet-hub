package com.batu.transaction_service.util;

import org.springframework.data.domain.KeysetScrollPosition;
import org.springframework.data.domain.ScrollPosition;
import org.springframework.stereotype.Component;

import com.batu.transaction_service.exception.CursorProcessingException;
import com.batu.transaction_service.exception.InvalidCursorException;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


@Component
public class CursorUtils {

    private final ObjectMapper objectMapper;

    public CursorUtils(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public ScrollPosition decode(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return ScrollPosition.keyset();
        }
        try {
            byte[] bytes = Base64.getDecoder().decode(cursor);
            Map<String, Object> rawKeys = objectMapper.readValue(bytes, new TypeReference<>() {});
            Map<String, Object> typedKeys = new HashMap<>();
            
            rawKeys.forEach((k, v) -> {
                typedKeys.put(k, convertToType(v));
            });

            return ScrollPosition.forward(typedKeys);
        } catch (Exception e) {
            throw new InvalidCursorException("Invalid cursor format", e);
        }
    }

    public String encode(ScrollPosition position) {
        if (position instanceof KeysetScrollPosition keyset && !keyset.isInitial()) {
            try {
                byte[] bytes = objectMapper.writeValueAsBytes(keyset.getKeys());
                return Base64.getEncoder().encodeToString(bytes);
            } catch (Exception e) {
                throw new CursorProcessingException("Failed to encode cursor", e);
            }
        }
        return null;
    }


    private Object convertToType(Object value) {
        if (value instanceof String str) {
            // Try UUID
            try {
                return UUID.fromString(str);
            } catch (IllegalArgumentException ignored) {}


            try {
                return LocalDateTime.parse(str);
            } catch (DateTimeParseException ignored) {}

        }
        return value;
    }
}
