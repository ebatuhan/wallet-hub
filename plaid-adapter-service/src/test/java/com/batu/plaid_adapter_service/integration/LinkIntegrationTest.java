package com.batu.plaid_adapter_service.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.Assertions;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.context.ActiveProfiles;


import org.wiremock.spring.EnableWireMock;

import com.batu.plaid_adapter_service.entity.Connection;

import com.batu.plaid_adapter_service.service.impl.ConnectionServiceImpl;
import com.batu.plaid_adapter_service.service.impl.LinkServiceImpl;
import com.batu.shared.dto.ExchangeTokenRequestDto;
import com.batu.shared.dto.ExhcangetokenResponseDto;
import com.batu.shared.dto.LinkTokenRequestDto;
import com.batu.shared.dto.LinkTokenResponseDto;
import com.plaid.client.ApiClient;

import com.plaid.client.request.PlaidApi;

import okhttp3.OkHttpClient;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

@SpringBootTest
@ActiveProfiles("test")
@EnableWireMock
public class LinkIntegrationTest {

    @Autowired
    private LinkServiceImpl linkServiceImpl;

    @Autowired
    private ConnectionServiceImpl connectionService;

    @TestConfiguration
    static class TestConfig {
        @Bean
        @Primary
        public PlaidApi plaidTestClient(@Value("${wiremock.server.baseUrl}") String wireMockUrl) {
            ApiClient apiClient = new ApiClient();

            apiClient.setPlaidAdapter(wireMockUrl);
            apiClient.configureFromOkclient(new OkHttpClient.Builder().build());

            PlaidApi plaidApiClient = apiClient.createService(PlaidApi.class);

            return plaidApiClient;
        }
    }

    @Test
    void createLinkToken_withValidData_returnsSuccess() {
        stubFor(post(urlPathEqualTo("/link/token/create"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "link_token": "link-sandbox-12345",
                                  "expiration": "2020-03-27T12:56:02Z",
                                  "request_id": "c7VjB"
                                }
                                """)));

        Jwt mockJwt = mock(Jwt.class);

        when(mockJwt.getSubject())
                .thenReturn("test-user");

        LinkTokenRequestDto request = new LinkTokenRequestDto();

        LinkTokenResponseDto response = linkServiceImpl.createLinkToken(request, mockJwt);

        Assertions.assertEquals("link-sandbox-12345", response.getLinkToken());
        verify(postRequestedFor(urlPathEqualTo("/link/token/create")));
    }

    @Test
    void exchangePublicToken_withValidData_returnsSuccess() {

        var expectedResonpose = aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(
                        """

                                {
                                "access_token": "mock-access-token",
                                "item_id": "mock-item-id",
                                "request_id": "mock-request-id"
                                }""");

        stubFor(
                post(urlPathEqualTo("/item/public_token/exchange"))
                        .willReturn(expectedResonpose));

        String mockPublicToken = "mock-public-token";
        String mockInsId = "mock-ins-id";
        String mockInsName = "mock-ins-name";

        ExchangeTokenRequestDto request = new ExchangeTokenRequestDto(mockPublicToken, mockInsId, mockInsName);

        Jwt mockJwt = mock(Jwt.class);

        when(mockJwt.getSubject())
                .thenReturn(UUID.randomUUID().toString());

        ExhcangetokenResponseDto response = linkServiceImpl.exchangeToken(request, mockJwt);
        verify(postRequestedFor(urlPathEqualTo("/item/public_token/exchange")));



        Connection passedConnection = connectionService.readAll().getFirst();
        assertEquals("mock-access-token", passedConnection.getAccessToken());
        assertEquals(mockInsId, passedConnection.getInstitutionId());
        assertEquals(mockInsId, response.getInstitutionId());
        assertEquals(mockInsName, response.getInstitutionName());

    }

}
