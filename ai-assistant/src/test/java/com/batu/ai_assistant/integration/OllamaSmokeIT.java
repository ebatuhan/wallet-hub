package com.batu.ai_assistant.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.Jwt;
import org.testcontainers.postgresql.PostgreSQLContainer;

import com.batu.ai_assistant.dto.ChatRequestDTO;
import com.batu.ai_assistant.service.AIAssistantService;

@EnabledIfEnvironmentVariable(named = "OLLAMA_IT_ENABLED", matches = "true")
@SpringBootTest(properties = {
        "spring.cloud.config.enabled=false",
        "spring.config.import=",
        "budgetingclient.url=http://localhost",
        "dashboardclient.url=http://localhost",
        "transactionclient.url=http://localhost",
        "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost/jwks",
        "spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost/issuer",
        "spring.ai.ollama.chat.options.model=${OLLAMA_IT_MODEL:qwen3}",
        "assistant.guard.model=${OLLAMA_IT_MODEL:qwen3}",
        "spring.ai.retry.max-attempts=1"
})
@Import(OllamaSmokeIT.TestcontainersConfiguration.class)
class OllamaSmokeIT {

    private static final UUID USER_ID = UUID.fromString("c3000000-0000-0000-0000-000000000001");

    @Autowired
    private AIAssistantService aiAssistantService;

    @Test
    void chat_whenOllamaIsEnabled_shouldReturnNonBlankResponse() {
        var response = aiAssistantService.chat(new ChatRequestDTO("Reply with exactly: ok"), jwt());

        assertThat(response.message()).isNotBlank();
    }

    private static Jwt jwt() {
        return Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject(USER_ID.toString())
                .build();
    }

    @org.springframework.boot.test.context.TestConfiguration(proxyBeanMethods = false)
    static class TestcontainersConfiguration {

        @Bean
        @ServiceConnection
        PostgreSQLContainer postgresContainer() {
            return new PostgreSQLContainer("postgres:18-alpine");
        }
    }
}
