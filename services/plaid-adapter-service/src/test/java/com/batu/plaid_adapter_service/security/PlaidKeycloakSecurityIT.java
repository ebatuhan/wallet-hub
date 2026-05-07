package com.batu.plaid_adapter_service.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import com.batu.plaid_adapter_service.entity.Connection;
import com.batu.plaid_adapter_service.repository.ConnectionRepository;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.cloud.config.enabled=false",
        "spring.config.import=",
        "spring.jpa.show-sql=false",
        "spring.jpa.properties.hibernate.show_sql=false",
        "spring.task.scheduling.enabled=false",
        "management.otlp.metrics.export.enabled=false",
        "plaid.api=test-client-id",
        "plaid.secret=test-secret",
        "plaid.env=sandbox",
        "accountclient.url=http://localhost/account-service",
        "transactionclient.url=http://localhost/transaction-service",
        "spring.security.oauth2.client.provider.keycloak.token-uri=http://localhost/token"
})
@Import(PlaidKeycloakSecurityIT.TestcontainersConfiguration.class)
class PlaidKeycloakSecurityIT {

    private static final int KEYCLOAK_PORT = 8080;
    private static final UUID USER_A_ID = UUID.fromString("85000000-0000-0000-0000-000000000001");
    private static final UUID USER_B_ID = UUID.fromString("85000000-0000-0000-0000-000000000002");
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

    @Autowired
    private ConnectionRepository connectionRepository;

    @DynamicPropertySource
    static void keycloakProperties(DynamicPropertyRegistry registry) {
        keycloakContainer.start();
        registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri", PlaidKeycloakSecurityIT::issuerUri);
        registry.add("spring.security.oauth2.resourceserver.jwt.jwk-set-uri",
                () -> issuerUri() + "/protocol/openid-connect/certs");
    }

    @Test
    void listConnections_whenNoTokenIsProvided_shouldReturnUnauthorized() throws Exception {
        HttpResponse<String> response = send(HttpRequest.newBuilder(appUri("/plaid/connections")).GET());

        assertThat(response.statusCode()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
    }

    @Test
    void listConnections_whenTokenIsValid_shouldReturnOnlyAuthenticatedUsersConnections() throws Exception {
        saveConnection(USER_A_ID, "item-user-a", "User A Bank", true);
        saveConnection(USER_B_ID, "item-user-b", "User B Bank", true);

        HttpResponse<String> response = send(authorizedRequest("/plaid/connections", tokenFor("plaid-user-a")).GET());

        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
        assertThat(response.body()).contains("User A Bank");
        assertThat(response.body()).doesNotContain("User B Bank");
    }

    @Test
    void getConnection_whenTokenBelongsToDifferentUser_shouldReturnNotFound() throws Exception {
        Connection userBConnection = saveConnection(USER_B_ID, "item-foreign", "Foreign Bank", true);

        HttpResponse<String> response = send(authorizedRequest(
                "/plaid/connections/" + userBConnection.getConnectionId(),
                tokenFor("plaid-user-a")).GET());

        assertThat(response.statusCode()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(response.body()).contains("not found");
    }

    @Test
    void webhook_whenNoTokenIsProvided_shouldBePermitted() throws Exception {
        HttpResponse<String> response = send(HttpRequest.newBuilder(appUri("/api/plaid/webhook"))
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .POST(HttpRequest.BodyPublishers.ofString("""
                        {
                          "webhook_type": "ITEM",
                          "webhook_code": "UNKNOWN",
                          "item_id": "item-1"
                        }
                        """)));

        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
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

    private Connection saveConnection(UUID userId, String externalId, String institutionName, boolean active) {
        Connection connection = new Connection(userId, externalId, "access-token-" + externalId, "ins-1",
                institutionName);
        connection.setActive(active);
        return connectionRepository.saveAndFlush(connection);
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
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode())
                .as(response.body())
                .isEqualTo(200);

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
            return new PostgreSQLContainer(DockerImageName.parse("postgres:16-alpine"));
        }
    }
}
