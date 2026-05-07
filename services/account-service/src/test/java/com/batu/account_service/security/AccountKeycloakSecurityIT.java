package com.batu.account_service.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
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
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import com.batu.account_service.entity.Account;
import com.batu.account_service.repository.AccountRepository;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.cloud.config.enabled=false",
        "spring.config.import=",
        "spring.rabbitmq.listener.simple.auto-startup=false",
        "spring.rabbitmq.listener.direct.auto-startup=false",
        "wallet-hub.outbox.relay-delay-ms=600000",
        "management.otlp.metrics.export.enabled=false",
        "spring.jpa.show-sql=false",
        "spring.jpa.properties.hibernate.show_sql=false"
})
@Import(AccountKeycloakSecurityIT.TestcontainersConfiguration.class)
class AccountKeycloakSecurityIT {

    private static final int KEYCLOAK_PORT = 8080;
    private static final UUID USER_A_ID = UUID.fromString("a4000000-0000-0000-0000-000000000001");
    private static final UUID CONNECTION_ID = UUID.fromString("a4000000-0000-0000-0000-000000000003");
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
    private AccountRepository accountRepository;

    @DynamicPropertySource
    static void keycloakProperties(DynamicPropertyRegistry registry) {
        keycloakContainer.start();
        registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri", AccountKeycloakSecurityIT::issuerUri);
        registry.add("spring.security.oauth2.resourceserver.jwt.jwk-set-uri",
                () -> issuerUri() + "/protocol/openid-connect/certs");
    }

    @BeforeEach
    void setUp() {
        accountRepository.deleteAll();
    }

    @Test
    void getAccounts_whenNoTokenIsProvided_shouldReturnUnauthorized() throws Exception {
        HttpResponse<String> response = send(HttpRequest.newBuilder(appUri("/accounts")).GET());

        assertThat(response.statusCode()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
    }

    @Test
    void getAccountSummary_whenTokenIsValid_shouldReturnAuthenticatedUsersSummary() throws Exception {
        accountRepository.saveAndFlush(account(USER_A_ID, CONNECTION_ID));

        HttpResponse<String> response = send(authorizedRequest("/accounts/summary", tokenFor("account-user-a")).GET());

        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
        assertThat(response.body()).contains(USER_A_ID.toString(), "USD", "activeAccountCount");
    }

    @Test
    void getAccountSummary_whenSubjectIsMalformed_shouldReturnBadRequest() throws Exception {
        HttpResponse<String> response = send(authorizedRequest("/accounts/summary", tokenFor("account-malformed-user")).GET());

        assertThat(response.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    }

    @Test
    void findInternalAccounts_whenNoTokenIsProvided_shouldReturnUnauthorized() throws Exception {
        HttpResponse<String> response = send(HttpRequest.newBuilder(appUri(
                "/accounts/internal/by-connection/" + CONNECTION_ID)).GET());

        assertThat(response.statusCode()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
    }

    @ParameterizedTest
    @MethodSource("internalEndpointRequests")
    void internalEndpoint_whenUserTokenHasNoServiceRole_shouldReturnForbidden(InternalEndpointRequest endpoint) throws Exception {
        HttpResponse<String> response = send(endpoint.authorizedRequest(port, tokenFor("account-user-a")));

        assertThat(response.statusCode()).isEqualTo(HttpStatus.FORBIDDEN.value());
    }

    @Test
    void findInternalAccounts_whenServiceRoleTokenIsValid_shouldReturnOk() throws Exception {
        accountRepository.saveAndFlush(account(USER_A_ID, CONNECTION_ID));

        HttpResponse<String> response = send(authorizedRequest(
                "/accounts/internal/by-connection/" + CONNECTION_ID,
                tokenFor("account-service-user")).GET());

        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
        assertThat(response.body()).contains("Checking", "Plaid Test Bank", "USD");
    }

    @Test
    void upsertInternalAccount_whenServiceRoleTokenIsValid_shouldReturnOk() throws Exception {
        HttpResponse<String> response = send(authorizedRequest("/accounts/internal/upsert", tokenFor("account-service-user"))
                .header(HttpHeaders.CONTENT_TYPE, "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(validUpsertJson())));

        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
        assertThat(response.body()).contains("Checking", USER_A_ID.toString(), CONNECTION_ID.toString());
    }

    @Test
    void deactivateInternalAccounts_whenServiceRoleTokenIsValid_shouldReturnOk() throws Exception {
        accountRepository.saveAndFlush(account(USER_A_ID, CONNECTION_ID));

        HttpResponse<String> response = send(authorizedRequest(
                "/accounts/internal/deactivate-by-connection/" + CONNECTION_ID,
                tokenFor("account-service-user")).PUT(HttpRequest.BodyPublishers.noBody()));

        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
        assertThat(response.body()).contains("Checking", USER_A_ID.toString(), CONNECTION_ID.toString(), "false");
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
        assertThat(response.statusCode())
                .as(response.body())
                .isEqualTo(HttpStatus.OK.value());

        var matcher = ACCESS_TOKEN_PATTERN.matcher(response.body());
        assertThat(matcher.find()).isTrue();
        return matcher.group(1);
    }

    private String form(String key, String value) {
        return URLEncoder.encode(key, StandardCharsets.UTF_8) + "="
                + URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static Stream<Arguments> internalEndpointRequests() {
        return Stream.of(
                Arguments.of(new InternalEndpointRequest("GET", "/accounts/internal/by-connection/" + CONNECTION_ID, null)),
                Arguments.of(new InternalEndpointRequest("POST", "/accounts/internal/upsert", validUpsertJson())),
                Arguments.of(new InternalEndpointRequest("PUT", "/accounts/internal/deactivate-by-connection/" + CONNECTION_ID, null)));
    }

    private static String validUpsertJson() {
        return """
                {
                  "accountId": "a4000000-0000-0000-0000-000000000004",
                  "userId": "a4000000-0000-0000-0000-000000000001",
                  "connectionId": "a4000000-0000-0000-0000-000000000003",
                  "institutionName": "Plaid Test Bank",
                  "accountName": "Checking",
                  "accountType": "depository",
                  "accountSubtype": "checking",
                  "accountMask": "1234",
                  "currentBalance": 100.00,
                  "availableBalance": 90.00,
                  "isoCurrencyCode": "USD"
                }
                """;
    }

    private record InternalEndpointRequest(String method, String path, String body) {

        HttpRequest.Builder authorizedRequest(int port, String token) {
            HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
            return switch (method) {
                case "GET" -> request.GET();
                case "POST" -> request.header(HttpHeaders.CONTENT_TYPE, "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(body));
                case "PUT" -> request.PUT(HttpRequest.BodyPublishers.noBody());
                default -> throw new IllegalArgumentException("Unsupported method " + method);
            };
        }
    }

    private Account account(UUID userId, UUID connectionId) {
        return new Account(
                UUID.randomUUID(),
                userId,
                connectionId,
                "Plaid Test Bank",
                "Checking",
                "depository",
                "checking",
                "1234",
                new BigDecimal("100.00"),
                new BigDecimal("90.00"),
                "USD",
                true);
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
