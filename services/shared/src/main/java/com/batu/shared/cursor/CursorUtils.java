package com.batu.shared.cursor;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.KeysetScrollPosition;
import org.springframework.data.domain.ScrollPosition;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

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
            Map<String, Object> rawKeys = objectMapper.readValue(bytes, new TypeReference<>() {
            });
            Map<String, Object> typedKeys = new HashMap<>();
            rawKeys.forEach((key, value) -> typedKeys.put(key, convertToType(value)));
            return ScrollPosition.forward(typedKeys);
        } catch (Exception exception) {
            throw new InvalidCursorException("Invalid cursor format", exception);
        }
    }

    public String encode(ScrollPosition position) {
        if (position instanceof KeysetScrollPosition keyset && !keyset.isInitial()) {
            try {
                byte[] bytes = objectMapper.writeValueAsBytes(keyset.getKeys());
                return Base64.getEncoder().encodeToString(bytes);
            } catch (Exception exception) {
                throw new CursorProcessingException("Failed to encode cursor", exception);
            }
        }
        return null;
    }

    private Object convertToType(Object value) {
        if (value instanceof String stringValue) {
            try {
                return UUID.fromString(stringValue);
            } catch (IllegalArgumentException ignored) {
            }

            try {
                return Instant.parse(stringValue);
            } catch (DateTimeParseException ignored) {
            }

            try {
                return LocalDateTime.parse(stringValue);
            } catch (DateTimeParseException ignored) {
            }
        }

        return value;
    }
}
