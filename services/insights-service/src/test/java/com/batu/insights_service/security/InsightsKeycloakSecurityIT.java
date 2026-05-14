package com.batu.insights_service.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.sql.Date;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.utility.DockerImageName;

import com.batu.insights_service.config.KeycloakConfiguration;
import com.batu.insights_service.controller.AccountInsightController;
import com.batu.insights_service.controller.TransactionInsightsController;
import com.batu.insights_service.service.AccountInsightsService;
import com.batu.insights_service.service.TransactionInsightsService;
import com.batu.shared.dto.response.AccountBalanceDataPointDto;
import com.batu.shared.dto.response.IncomeSummaryResponseDto;
import com.batu.shared.dto.response.SpendingCurrencyGroupDto;
import com.batu.shared.dto.response.SpendingPerCategoryDto;
import com.batu.shared.dto.response.SpendingPerCategoryResponseDto;
import com.batu.shared.error.CommonApplicationErrorAdvice;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = InsightsKeycloakSecurityIT.TestApplication.class,
        properties = {
                "spring.cloud.config.enabled=false",
                "spring.config.import=",
                "management.otlp.metrics.export.enabled=false"
        })
class InsightsKeycloakSecurityIT {

    private static final int KEYCLOAK_PORT = 8080;
    private static final UUID USER_ID = UUID.fromString("9d000000-0000-0000-0000-000000000001");
    private static final UUID ACCOUNT_ID = UUID.fromString("9d000000-0000-0000-0000-000000000002");
    private static final UUID CATEGORY_ID = UUID.fromString("9d000000-0000-0000-0000-000000000003");
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

    @MockitoBean
    private TransactionInsightsService transactionInsightsService;

    @MockitoBean
    private AccountInsightsService accountInsightsService;

    @DynamicPropertySource
    static void keycloakProperties(DynamicPropertyRegistry registry) {
        keycloakContainer.start();
        registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri", InsightsKeycloakSecurityIT::issuerUri);
        registry.add("spring.security.oauth2.resourceserver.jwt.jwk-set-uri",
                () -> issuerUri() + "/protocol/openid-connect/certs");
    }

    @ParameterizedTest
    @MethodSource("protectedGetEndpoints")
    void protectedEndpoint_whenNoTokenIsProvided_shouldReturnUnauthorized(String path) throws Exception {
        HttpResponse<String> response = send(HttpRequest.newBuilder(appUri(path)).GET());

        assertThat(response.statusCode()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
    }

    @Test
    void getSpendingByCategory_whenTokenIsValid_shouldReturnOkAndPassRealJwt() throws Exception {
        when(transactionInsightsService.getSpendingByCategory(any(String.class), any(Jwt.class)))
                .thenAnswer(invocation -> {
                    Jwt jwt = invocation.getArgument(1);
                    return new SpendingPerCategoryResponseDto(
                            UUID.fromString(jwt.getSubject()),
                            List.of(new SpendingCurrencyGroupDto(
                                    "USD",
                                    new BigDecimal("42.50"),
                                    List.of(new SpendingPerCategoryDto(
                                            CATEGORY_ID,
                                            new BigDecimal("100.00"),
                                            new BigDecimal("42.50"))))));
                });

        HttpResponse<String> response = send(authorizedRequest(
                "/api/insights/spendings?from=2026-04",
                tokenFor("insights-user-a")).GET());

        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
        assertThat(response.body()).contains(USER_ID.toString(), "USD", CATEGORY_ID.toString());
        verify(transactionInsightsService).getSpendingByCategory(any(String.class), any(Jwt.class));
    }

    @Test
    void getIncome_whenBearerTokenIsInvalid_shouldReturnUnauthorized() throws Exception {
        HttpResponse<String> response = send(authorizedRequest(
                "/api/insights/income?from=2026-04-01&to=2026-04-30",
                "not-a-real-token").GET());

        assertThat(response.statusCode()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
    }

    @Test
    void getAccountBalanceHistory_whenTokenIsValid_shouldReturnOkAndPassRealJwt() throws Exception {
        when(accountInsightsService.getAccountBalanceHistory(any(UUID.class), any(LocalDate.class), any(LocalDate.class), any(Jwt.class)))
                .thenAnswer(invocation -> {
                    Jwt jwt = invocation.getArgument(3);
                    return List.of(new AccountBalanceDataPointDto(
                            ACCOUNT_ID,
                            UUID.fromString(jwt.getSubject()),
                            new BigDecimal("120.00"),
                            "USD",
                            LocalDate.of(2026, 4, 1)));
                });

        HttpResponse<String> response = send(authorizedRequest(
                "/api/insights/accounts/%s/balance-history?from=2026-04-01&to=2026-04-30".formatted(ACCOUNT_ID),
                tokenFor("insights-user-a")).GET());

        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
        assertThat(response.body()).contains(USER_ID.toString(), ACCOUNT_ID.toString(), "120.00", "USD");
        verify(accountInsightsService).getAccountBalanceHistory(any(UUID.class), any(LocalDate.class), any(LocalDate.class), any(Jwt.class));
    }

    private static Stream<Arguments> protectedGetEndpoints() {
        return Stream.of(
                Arguments.of("/api/insights/spendings?from=2026-04"),
                Arguments.of("/api/insights/spendings/graph?from=2026-04"),
                Arguments.of("/api/insights/income?from=2026-04-01&to=2026-04-30"),
                Arguments.of("/api/insights/accounts/%s/balance-history?from=2026-04-01&to=2026-04-30".formatted(ACCOUNT_ID)));
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

    @SpringBootConfiguration
    @EnableAutoConfiguration(excludeName = {
            "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration",
            "org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration",
            "org.springframework.boot.amqp.autoconfigure.RabbitAutoConfiguration"
    })
    @Import({ KeycloakConfiguration.class, TransactionInsightsController.class, AccountInsightController.class,
            CommonApplicationErrorAdvice.class })
    static class TestApplication {
    }
}
