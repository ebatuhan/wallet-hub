package com.batu.transaction_service.security;

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
import java.time.LocalDate;
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

import com.batu.transaction_service.entity.Transaction;
import com.batu.transaction_service.entity.TransactionDetailedCategory;
import com.batu.transaction_service.entity.TransactionPrimaryCategory;
import com.batu.transaction_service.repository.DetailedCategoryRepository;
import com.batu.transaction_service.repository.InboxEventRepository;
import com.batu.transaction_service.repository.OutboxEventRepository;
import com.batu.transaction_service.repository.PrimaryCategoryRepository;
import com.batu.transaction_service.repository.TransactionRepository;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.cloud.config.enabled=false",
        "spring.config.import=",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.rabbitmq.listener.simple.auto-startup=false",
        "spring.rabbitmq.listener.direct.auto-startup=false",
        "wallet-hub.outbox.relay-delay-ms=600000",
        "spring.task.scheduling.enabled=false",
        "management.otlp.metrics.export.enabled=false",
        "spring.jpa.show-sql=false",
        "spring.jpa.properties.hibernate.show_sql=false"
})
@Import(TransactionKeycloakSecurityIT.TestcontainersConfiguration.class)
class TransactionKeycloakSecurityIT {

    private static final int KEYCLOAK_PORT = 8080;
    private static final UUID USER_A_ID = UUID.fromString("a4000000-0000-0000-0000-000000000001");
    private static final UUID ACCOUNT_ID = UUID.fromString("a5000000-0000-0000-0000-000000000001");
    private static final UUID TRANSACTION_ID = UUID.fromString("a5000000-0000-0000-0000-000000000002");
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
    private TransactionRepository transactionRepository;

    @Autowired
    private PrimaryCategoryRepository primaryCategoryRepository;

    @Autowired
    private DetailedCategoryRepository detailedCategoryRepository;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private InboxEventRepository inboxEventRepository;

    @DynamicPropertySource
    static void keycloakProperties(DynamicPropertyRegistry registry) {
        keycloakContainer.start();
        registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri", TransactionKeycloakSecurityIT::issuerUri);
        registry.add("spring.security.oauth2.resourceserver.jwt.jwk-set-uri",
                () -> issuerUri() + "/protocol/openid-connect/certs");
    }

    @BeforeEach
    void setUp() {
        outboxEventRepository.deleteAll();
        inboxEventRepository.deleteAll();
        transactionRepository.deleteAll();
        detailedCategoryRepository.deleteAll();
        primaryCategoryRepository.deleteAll();
    }

    @Test
    void getTransactions_whenNoTokenIsProvided_shouldReturnUnauthorized() throws Exception {
        HttpResponse<String> response = send(HttpRequest.newBuilder(appUri("/transactions")).GET());

        assertThat(response.statusCode()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
    }

    @Test
    void getTransactions_whenTokenIsValid_shouldReturnAuthenticatedUsersTransactions() throws Exception {
        TransactionDetailedCategory category = saveCategory();
        transactionRepository.saveAndFlush(transaction(TRANSACTION_ID, USER_A_ID, ACCOUNT_ID, category, true));

        HttpResponse<String> response = send(authorizedRequest("/transactions", tokenFor("transaction-user-a")).GET());

        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
        assertThat(response.body()).contains(TRANSACTION_ID.toString(), "Coffee Shop", "FOOD_AND_DRINK");
    }

    @Test
    void getTransactions_whenSubjectIsMalformed_shouldReturnBadRequest() throws Exception {
        HttpResponse<String> response = send(authorizedRequest("/transactions", tokenFor("transaction-malformed-user")).GET());

        assertThat(response.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    }

    @Test
    void getPrimaryCategories_whenNoTokenIsProvided_shouldReturnOkBecauseRouteIsPublic() throws Exception {
        saveCategory();

        HttpResponse<String> response = send(HttpRequest.newBuilder(appUri("/transactions/categories/primary")).GET());

        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
        assertThat(response.body()).contains("FOOD_AND_DRINK");
    }

    @Test
    void upsertInternalTransaction_whenNoTokenIsProvided_shouldReturnUnauthorized() throws Exception {
        HttpResponse<String> response = send(HttpRequest.newBuilder(appUri("/transactions/internal/upsert"))
                .header(HttpHeaders.CONTENT_TYPE, "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(validUpsertJson())));

        assertThat(response.statusCode()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
    }

    @ParameterizedTest
    @MethodSource("internalEndpointRequests")
    void internalEndpoint_whenUserTokenHasNoServiceRole_shouldReturnForbidden(InternalEndpointRequest endpoint) throws Exception {
        HttpResponse<String> response = send(endpoint.authorizedRequest(port, tokenFor("transaction-user-a")));

        assertThat(response.statusCode()).isEqualTo(HttpStatus.FORBIDDEN.value());
    }

    @Test
    void upsertInternalTransaction_whenServiceRoleTokenIsValid_shouldReturnOk() throws Exception {
        saveCategory();

        HttpResponse<String> response = send(authorizedRequest("/transactions/internal/upsert", tokenFor("transaction-service-user"))
                .header(HttpHeaders.CONTENT_TYPE, "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(validUpsertJson())));

        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
        assertThat(response.body()).contains(TRANSACTION_ID.toString(), USER_A_ID.toString(), ACCOUNT_ID.toString(), "true");
    }

    @Test
    void deactivateInternalTransactions_whenServiceRoleTokenIsValid_shouldReturnOk() throws Exception {
        TransactionDetailedCategory category = saveCategory();
        transactionRepository.saveAndFlush(transaction(TRANSACTION_ID, USER_A_ID, ACCOUNT_ID, category, true));

        HttpResponse<String> response = send(authorizedRequest(
                "/transactions/internal/deactivate-by-account/" + ACCOUNT_ID,
                tokenFor("transaction-service-user")).PUT(HttpRequest.BodyPublishers.noBody()));

        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
        assertThat(response.body()).contains(TRANSACTION_ID.toString(), USER_A_ID.toString(), ACCOUNT_ID.toString(), "false");
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
                Arguments.of(new InternalEndpointRequest("POST", "/transactions/internal/upsert", validUpsertJson())),
                Arguments.of(new InternalEndpointRequest("PUT", "/transactions/internal/deactivate-by-account/" + ACCOUNT_ID, null)));
    }

    private static String validUpsertJson() {
        return """
                {
                  "transactionId": "a5000000-0000-0000-0000-000000000002",
                  "userId": "a4000000-0000-0000-0000-000000000001",
                  "accountId": "a5000000-0000-0000-0000-000000000001",
                  "amount": 42.50,
                  "isoCurrencyCode": "USD",
                  "transactionName": "Coffee Shop",
                  "transactionType": "place",
                  "date": "2026-05-07",
                  "pending": false,
                  "paymentChannel": "in store",
                  "detailedCategoryCode": "FOOD_AND_DRINK_COFFEE",
                  "active": true
                }
                """;
    }

    private record InternalEndpointRequest(String method, String path, String body) {

        HttpRequest.Builder authorizedRequest(int port, String token) {
            HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
            return switch (method) {
                case "POST" -> request.header(HttpHeaders.CONTENT_TYPE, "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(body));
                case "PUT" -> request.PUT(HttpRequest.BodyPublishers.noBody());
                default -> throw new IllegalArgumentException("Unsupported method " + method);
            };
        }
    }

    private TransactionDetailedCategory saveCategory() {
        TransactionPrimaryCategory primaryCategory = primaryCategoryRepository.saveAndFlush(
                new TransactionPrimaryCategory("FOOD_AND_DRINK", "Food And Drink", "food.svg"));
        return detailedCategoryRepository.saveAndFlush(new TransactionDetailedCategory(
                "Coffee",
                "FOOD_AND_DRINK_COFFEE",
                primaryCategory,
                "Coffee shops"));
    }

    private Transaction transaction(
            UUID transactionId,
            UUID userId,
            UUID accountId,
            TransactionDetailedCategory category,
            boolean active) {
        return new Transaction(
                transactionId,
                userId,
                accountId,
                new BigDecimal("42.50"),
                "USD",
                "Coffee Shop",
                "place",
                LocalDate.of(2026, 5, 7),
                false,
                "in store",
                category,
                active);
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
