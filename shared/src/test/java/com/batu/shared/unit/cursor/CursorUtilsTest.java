package com.batu.shared.unit.cursor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.KeysetScrollPosition;
import org.springframework.data.domain.ScrollPosition;

import com.batu.shared.cursor.CursorUtils;
import com.batu.shared.cursor.InvalidCursorException;

import tools.jackson.databind.json.JsonMapper;

class CursorUtilsTest {

    private CursorUtils cursorUtils;

    @BeforeEach
    void setUp() {
        cursorUtils = new CursorUtils(new JsonMapper());
    }

    @Test
    void decode_whenCursorIsNull_shouldReturnInitialKeysetPosition() {
        ScrollPosition position = cursorUtils.decode(null);

        assertThat(position).isInstanceOf(KeysetScrollPosition.class);
        assertThat(((KeysetScrollPosition) position).isInitial()).isTrue();
    }

    @Test
    void decode_whenCursorIsBlank_shouldReturnInitialKeysetPosition() {
        ScrollPosition position = cursorUtils.decode("   ");

        assertThat(position).isInstanceOf(KeysetScrollPosition.class);
        assertThat(((KeysetScrollPosition) position).isInitial()).isTrue();
    }

    @Test
    void decode_whenCursorContainsKnownTypes_shouldConvertStringValuesToTypedKeys() {
        UUID accountId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        Instant createdAt = Instant.parse("2026-05-06T10:15:30Z");
        LocalDateTime postedAt = LocalDateTime.parse("2026-05-06T13:15:30");
        String cursor = encodeJson("""
                {
                  "accountId": "11111111-1111-1111-1111-111111111111",
                  "createdAt": "2026-05-06T10:15:30Z",
                  "postedAt": "2026-05-06T13:15:30",
                  "amount": 25
                }
                """);

        KeysetScrollPosition position = (KeysetScrollPosition) cursorUtils.decode(cursor);

        assertThat(position.isInitial()).isFalse();
        assertThat(position.getKeys())
                .containsEntry("accountId", accountId)
                .containsEntry("createdAt", createdAt)
                .containsEntry("postedAt", postedAt)
                .containsEntry("amount", 25);
    }

    @Test
    void decode_whenCursorIsMalformedBase64_shouldThrowInvalidCursorException() {
        assertThatThrownBy(() -> cursorUtils.decode("not-base64!!"))
                .isInstanceOf(InvalidCursorException.class)
                .hasMessageContaining("Invalid cursor format");
    }

    @Test
    void decode_whenCursorIsMalformedJson_shouldThrowInvalidCursorException() {
        String cursor = encodeJson("not-json");

        assertThatThrownBy(() -> cursorUtils.decode(cursor))
                .isInstanceOf(InvalidCursorException.class)
                .hasMessageContaining("Invalid cursor format");
    }

    @Test
    void encode_whenPositionIsInitial_shouldReturnNull() {
        assertThat(cursorUtils.encode(ScrollPosition.keyset())).isNull();
    }

    @Test
    void encode_whenPositionHasKeys_shouldReturnRoundTrippableCursor() {
        UUID accountId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        ScrollPosition position = ScrollPosition.forward(Map.of(
                "accountId", accountId.toString(),
                "createdAt", "2026-05-06T10:15:30Z"));

        String encoded = cursorUtils.encode(position);

        assertThat(encoded).isNotBlank();
        KeysetScrollPosition decoded = (KeysetScrollPosition) cursorUtils.decode(encoded);
        assertThat(decoded.getKeys())
                .containsEntry("accountId", accountId)
                .containsEntry("createdAt", Instant.parse("2026-05-06T10:15:30Z"));
    }

    private String encodeJson(String json) {
        return Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
    }
}
