package com.batu.ai_assistant.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.cloud.config.enabled=false",
        "spring.config.import=",
        "budgetingclient.url=http://localhost",
        "dashboardclient.url=http://localhost",
        "transactionclient.url=http://localhost",
        "spring.ai.ollama.chat.options.model=qwen3",
        "spring.ai.ollama.init.pull-model-strategy=never",
        "assistant.guard.model=qwen3",
        "management.otlp.metrics.export.enabled=false",
        "spring.jpa.show-sql=false",
        "spring.jpa.properties.hibernate.show_sql=false"
})
@Import(AiAssistantKeycloakSecurityIT.TestcontainersConfiguration.class)
class AiAssistantKeycloakSecurityIT {

    private static final int KEYCLOAK_PORT = 8080;
    private static final Pattern ACCESS_TOKEN_PATTERN = Pattern.compile("\\\"access_token\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"");

    private static final GenericContainer<?> keycloakContainer = new GenericContainer<>(
            DockerImageName.parse("quay.io/keycloak/keycloak:26.5.2"))
            .withExposedPorts(KEYCLOAK_PORT)
            .withEnv("KC_BOOTSTRAP_ADMIN_USERNAME", "admin")
            .withEnv("KC_BOOTSTRAP_ADMIN_PASSWORD", "admin")
            .withCopyToContainer(realmJson(), "/opt/keycloak/data/import/wallet-hub-realm.json")
            .withCommand("start-dev", "--import-realm")
            .waitingFor(Wait.forHttp("/realms/wallet-hub/.well-known/openid-configuration")
                    .forPort(KEYCLOAK_PORT)
                    .forStatusCode(200)
                    .withStartupTimeout(Duration.ofMinutes(2)));

    @LocalServerPort
    private int port;

    @DynamicPropertySource
    static void keycloakProperties(DynamicPropertyRegistry registry) {
        keycloakContainer.start();
        registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri", AiAssistantKeycloakSecurityIT::issuerUri);
        registry.add("spring.security.oauth2.resourceserver.jwt.jwk-set-uri",
                () -> issuerUri() + "/protocol/openid-connect/certs");
    }

    @Test
    void history_whenNoTokenIsProvided_shouldReturnUnauthorized() throws Exception {
        HttpResponse<String> response = send(HttpRequest.newBuilder(appUri("/assistant/chat/history")).GET());

        assertThat(response.statusCode()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
    }

    @Test
    void history_whenTokenIsValid_shouldReturnOk() throws Exception {
        HttpResponse<String> response = send(authorizedRequest(
                "/assistant/chat/history",
                tokenFor("assistant-user-a")).GET());

        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
        assertThat(response.body()).contains("\"status\":\"IDLE\"", "\"data\":[]");
    }

    @Test
    void history_whenSubjectIsMalformed_shouldReturnBadRequest() throws Exception {
        HttpResponse<String> response = send(authorizedRequest(
                "/assistant/chat/history",
                tokenFor("assistant-malformed-user")).GET());

        assertThat(response.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    }

    @Test
    void chat_whenNoTokenIsProvided_shouldReturnUnauthorized() throws Exception {
        HttpResponse<String> response = send(HttpRequest.newBuilder(appUri("/assistant/chat"))
                .header(HttpHeaders.CONTENT_TYPE, "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"message\":\"Hello\"}")));

        assertThat(response.statusCode()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
    }

    private static Transferable realmJson() {
        try {
            return Transferable.of(new ClassPathResource("keycloak/wallet-hub-realm.json")
                    .getContentAsByteArray());
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to load Keycloak test realm", exception);
        }
    }

    private static String issuerUri() {
        return "http://%s:%d/realms/wallet-hub".formatted(
                keycloakContainer.getHost(),
                keycloakContainer.getMappedPort(KEYCLOAK_PORT));
    }

    private HttpRequest.Builder authorizedRequest(String path, String token) {
        return HttpRequest.newBuilder(appUri(path))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
    }

    private URI appUri(String path) {
        return URI.create("http://localhost:" + port + path);
    }

    private HttpResponse<String> send(HttpRequest.Builder request) throws IOException, InterruptedException {
        return HttpClient.newHttpClient().send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    private String tokenFor(String username) throws IOException, InterruptedException {
        String body = form("grant_type", "password")
                + "&" + form("client_id", "wallet-hub-test")
                + "&" + form("username", username)
                + "&" + form("password", "password");
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(issuerUri() + "/protocol/openid-connect/token"))
                .header(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).as(response.body()).isEqualTo(HttpStatus.OK.value());

        var matcher = ACCESS_TOKEN_PATTERN.matcher(response.body());
        assertThat(matcher.find()).isTrue();
        return matcher.group(1);
    }

    private String form(String key, String value) {
        return URLEncoder.encode(key, StandardCharsets.UTF_8) + "="
                + URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class TestcontainersConfiguration {

        @Bean
        @ServiceConnection
        PostgreSQLContainer postgresContainer() {
            return new PostgreSQLContainer(DockerImageName.parse("postgres:18-alpine"));
        }

        @Bean
        ChatClient.Builder chatClientBuilder() {
            ChatClient.Builder builder = mock(ChatClient.Builder.class);
            when(builder.defaultSystem(anyString())).thenReturn(builder);
            when(builder.defaultTools(any(Object.class), any(Object.class), any(Object.class))).thenReturn(builder);
            when(builder.defaultAdvisors(any(Advisor.class), any(Advisor.class), any(Advisor.class))).thenReturn(builder);
            when(builder.defaultOptions(any(OllamaChatOptions.class))).thenReturn(builder);
            when(builder.build()).thenReturn(mock(ChatClient.class));
            return builder;
        }
    }
}
