package com.batu.budgeting.security;

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

import com.batu.budgeting.entity.Budget;
import com.batu.budgeting.entity.BudgetPeriod;
import com.batu.budgeting.repository.BudgetRepository;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.cloud.config.enabled=false",
        "spring.config.import=",
        "spring.rabbitmq.listener.simple.auto-startup=false",
        "management.otlp.metrics.export.enabled=false",
        "spring.jpa.show-sql=false",
        "spring.jpa.properties.hibernate.show_sql=false"
})
@Import(BudgetKeycloakSecurityIT.TestcontainersConfiguration.class)
class BudgetKeycloakSecurityIT {

    private static final int KEYCLOAK_PORT = 8080;
    private static final UUID USER_A_ID = UUID.fromString("b0000000-0000-0000-0000-000000000001");
    private static final UUID USER_B_ID = UUID.fromString("b0000000-0000-0000-0000-000000000002");
    private static final UUID USER_A_CATEGORY_ID = UUID.fromString("b0000000-0000-0000-0000-000000000003");
    private static final UUID USER_B_CATEGORY_ID = UUID.fromString("b0000000-0000-0000-0000-000000000004");
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
    private BudgetRepository budgetRepository;

    @DynamicPropertySource
    static void keycloakProperties(DynamicPropertyRegistry registry) {
        keycloakContainer.start();
        registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri", BudgetKeycloakSecurityIT::issuerUri);
        registry.add("spring.security.oauth2.resourceserver.jwt.jwk-set-uri",
                () -> issuerUri() + "/protocol/openid-connect/certs");
    }

    @Test
    void getBudgets_whenNoTokenIsProvided_shouldReturnUnauthorized() throws Exception {
        HttpResponse<String> response = send(HttpRequest.newBuilder(appUri("/budgets")).GET());

        assertThat(response.statusCode()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
    }

    @Test
    void getBudgets_whenTokenIsValid_shouldReturnOnlyAuthenticatedUsersBudgets() throws Exception {
        saveBudget(USER_A_ID, USER_A_CATEGORY_ID);
        saveBudget(USER_B_ID, USER_B_CATEGORY_ID);

        HttpResponse<String> response = send(authorizedRequest("/budgets", tokenFor("budget-user-a")).GET());

        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
        assertThat(response.body()).contains(USER_A_CATEGORY_ID.toString());
        assertThat(response.body()).doesNotContain(USER_B_CATEGORY_ID.toString());
    }

    @Test
    void updateBudget_whenTokenBelongsToDifferentUser_shouldReturnNotFoundAndNotMutateBudget() throws Exception {
        Budget userBBudget = saveBudget(USER_B_ID, USER_B_CATEGORY_ID);

        HttpResponse<String> response = send(authorizedRequest("/budgets/" + userBBudget.getId(), tokenFor("budget-user-a"))
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .PUT(HttpRequest.BodyPublishers.ofString(updateRequestJson())));

        assertThat(response.statusCode()).isEqualTo(HttpStatus.NOT_FOUND.value());
        Budget unchangedBudget = budgetRepository.findById(userBBudget.getId()).orElseThrow();
        assertThat(unchangedBudget.getCategoryId()).isEqualTo(USER_B_CATEGORY_ID);
        assertThat(unchangedBudget.getIsoCurrencyCode()).isEqualTo("USD");
        assertThat(unchangedBudget.getPeriod()).isEqualTo(BudgetPeriod.MONTHLY);
    }

    @Test
    void deactivateBudget_whenTokenBelongsToDifferentUser_shouldReturnNotFoundAndNotMutateBudget() throws Exception {
        Budget userBBudget = saveBudget(USER_B_ID, USER_B_CATEGORY_ID);

        HttpResponse<String> response = send(authorizedRequest("/budgets/" + userBBudget.getId(), tokenFor("budget-user-a"))
                .DELETE());

        assertThat(response.statusCode()).isEqualTo(HttpStatus.NOT_FOUND.value());
        Budget unchangedBudget = budgetRepository.findById(userBBudget.getId()).orElseThrow();
        assertThat(unchangedBudget.isActive()).isTrue();
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

    private Budget saveBudget(UUID userId, UUID categoryId) {
        return budgetRepository.saveAndFlush(new Budget(
                userId,
                categoryId,
                new BigDecimal("500.00"),
                "USD",
                BudgetPeriod.MONTHLY,
                LocalDate.of(2026, 5, 1)));
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

    private String updateRequestJson() {
        return """
                {
                  "categoryId": "b0000000-0000-0000-0000-000000000003",
                  "limitAmount": 900.00,
                  "isoCurrencyCode": "EUR",
                  "period": "WEEKLY",
                  "periodStart": "2026-06-01"
                }
                """;
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class TestcontainersConfiguration {

        @Bean
        @ServiceConnection
        PostgreSQLContainer postgresContainer() {
            return new PostgreSQLContainer(DockerImageName.parse("postgres:18-alpine"));
        }
    }
}
