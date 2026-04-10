package com.batu.plaid_adapter_service.unit.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Collections;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.util.ReflectionTestUtils;

import com.batu.plaid_adapter_service.entity.Connection;
import com.batu.plaid_adapter_service.exception.PlaidClientException;
import com.batu.plaid_adapter_service.service.ConnectionService;
import com.batu.plaid_adapter_service.service.impl.PlaidIntegrationServiceImpl;
import com.batu.shared.dto.request.ExchangeTokenRequestDto;
import com.batu.shared.dto.request.LinkTokenRequestDto;
import com.batu.shared.dto.response.ExchangeTokenResponseDto;
import com.batu.shared.dto.response.LinkTokenResponseDto;
import com.plaid.client.model.ItemPublicTokenExchangeRequest;
import com.plaid.client.model.ItemPublicTokenExchangeResponse;
import com.plaid.client.model.LinkTokenCreateRequest;
import com.plaid.client.model.LinkTokenCreateResponse;
import com.plaid.client.request.PlaidApi;

import okhttp3.MediaType;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;

@ExtendWith(MockitoExtension.class)
public class LinkServiceTest {

    @Mock
    private PlaidApi plaidClient;

    @Mock
    private ConnectionService connectionService;

    @Mock
    private Call<LinkTokenCreateResponse> mockLinkTokenCall;

    @Mock
    private Call<ItemPublicTokenExchangeResponse> mockExchangeTokenCall;

    @Mock
    private Jwt principal;

    @InjectMocks
    private PlaidIntegrationServiceImpl linkService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(linkService, "webhookUrl", "https://test-webhook.com");
    }

    @Test
    void createLinkToken_sucesfullResponse_returnsDto() throws IOException {
        String expectedLinkToken = "test-link-token";
        LinkTokenCreateResponse sucessResponse = new LinkTokenCreateResponse();
        sucessResponse.setLinkToken(expectedLinkToken);

        when(principal.getSubject()).thenReturn("test-user");
        when(plaidClient.linkTokenCreate(any(LinkTokenCreateRequest.class)))
                .thenReturn(mockLinkTokenCall);
        when(mockLinkTokenCall.execute())
                .thenReturn(Response.success(sucessResponse));

        LinkTokenResponseDto result = linkService.createLinkToken(new LinkTokenRequestDto(), principal);

        assertNotNull(result);
        assertEquals(expectedLinkToken, result.getLinkToken());
    }

    @Test
    void createLinkToken_PlaidApiError() throws IOException {
        String errorJson = "{"
                + "\"display_message\": \"Something went wrong with the bank\","
                + "\"error_code\": \"INSTITUTION_DOWN\","
                + "\"error_type\": \"INSTITUTION_ERROR\""
                + "}";

        ResponseBody errorBody = ResponseBody.create(errorJson, MediaType.parse("application/json"));

        when(principal.getSubject()).thenReturn("test-user");
        when(plaidClient.linkTokenCreate(any())).thenReturn(mockLinkTokenCall);
        when(mockLinkTokenCall.execute())
                .thenReturn(Response.error(400, errorBody));

        PlaidClientException exception = assertThrows(PlaidClientException.class, () -> {
            linkService.createLinkToken(new LinkTokenRequestDto(), principal);
        });

        assertEquals("Something went wrong with the bank", exception.getMessage());
        assertEquals(HttpStatus.BAD_GATEWAY, exception.getHttpStatus());
    }

    @Test
    void createLinkToken_NetworkError() throws IOException {
        when(principal.getSubject()).thenReturn("test-user");
        when(plaidClient.linkTokenCreate(any())).thenReturn(mockLinkTokenCall);
        when(mockLinkTokenCall.execute()).thenThrow(new IOException("Network failure"));

        PlaidClientException exception = assertThrows(PlaidClientException.class, () -> {
            linkService.createLinkToken(new LinkTokenRequestDto(), principal);
        });

        assertEquals("Unable to connect to banking provider.", exception.getMessage());
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exception.getHttpStatus());
    }

    @Test
    void createLinkToken_emptyErrorBody() throws IOException {
        String errorJson = "{"
                + "\"error_code\": \"INSTITUTION_DOWN\","
                + "\"error_type\": \"INSTITUTION_ERROR\""
                + "}";

        ResponseBody errorBody = ResponseBody.create(errorJson, MediaType.parse("application/json"));
        
        when(principal.getSubject()).thenReturn("test-user");
        when(plaidClient.linkTokenCreate(any())).thenReturn(mockLinkTokenCall);
        when(mockLinkTokenCall.execute())
                .thenReturn(Response.error(400, errorBody));

        PlaidClientException exception = assertThrows(PlaidClientException.class, () -> {
            linkService.createLinkToken(new LinkTokenRequestDto(), principal);
        });

        assertEquals("An error occurred in Plaid", exception.getMessage());
        assertEquals(HttpStatus.BAD_GATEWAY, exception.getHttpStatus());
    }

    @Test
    void exchangeToken_successfulResponse_returnsDto() throws IOException {
        String validUuid = UUID.randomUUID().toString();
        String publicToken = "public-token-123";
        String accessToken = "access-token-123";
        String itemId = "item-id-123";
        String institutionId = "ins_123";
        String institutionName = "Chase";

        ExchangeTokenRequestDto requestDto = new ExchangeTokenRequestDto(
                publicToken,
                Collections.emptyList(),
                institutionId,
                institutionName
        );

        when(principal.getSubject()).thenReturn(validUuid);

        ItemPublicTokenExchangeResponse successResponse = new ItemPublicTokenExchangeResponse();
        successResponse.setAccessToken(accessToken);
        successResponse.setItemId(itemId);

        when(plaidClient.itemPublicTokenExchange(any(ItemPublicTokenExchangeRequest.class)))
                .thenReturn(mockExchangeTokenCall);
        when(mockExchangeTokenCall.execute())
                .thenReturn(Response.success(successResponse));

        Connection savedConnection = new Connection(
                UUID.fromString(validUuid), itemId, accessToken, institutionId, institutionName);
        when(connectionService.create(any(Connection.class))).thenReturn(savedConnection);

        ExchangeTokenResponseDto result = linkService.exchangeToken(requestDto, principal);

        assertNotNull(result);
        assertEquals(institutionId, result.getInstitutionId());
        assertEquals(institutionName, result.getInstitutionName());

        verify(connectionService).create(any(Connection.class));
    }

    @Test
    void exchangeToken_PlaidApiError() throws IOException {
        ExchangeTokenRequestDto requestDto = new ExchangeTokenRequestDto(
                "bad-token",
                Collections.emptyList(),
                "ins_123",
                "Bank"
        );

        String errorJson = "{"
                + "\"display_message\": \"Invalid public token\","
                + "\"error_code\": \"INVALID_PUBLIC_TOKEN\","
                + "\"error_type\": \"INVALID_INPUT\""
                + "}";
        ResponseBody errorBody = ResponseBody.create(errorJson, MediaType.parse("application/json"));

        when(plaidClient.itemPublicTokenExchange(any(ItemPublicTokenExchangeRequest.class)))
                .thenReturn(mockExchangeTokenCall);
        when(mockExchangeTokenCall.execute())
                .thenReturn(Response.error(400, errorBody));

        PlaidClientException exception = assertThrows(PlaidClientException.class, () -> {
            linkService.exchangeToken(requestDto, principal);
        });

        assertEquals("Invalid public token", exception.getMessage());
        assertEquals(HttpStatus.BAD_GATEWAY, exception.getHttpStatus());
    }

    @Test
    void exchangeToken_PlaidApiError_DefaultMessage() throws IOException {
        ExchangeTokenRequestDto requestDto = new ExchangeTokenRequestDto(
                "bad-token",
                Collections.emptyList(),
                "ins_123",
                "Bank"
        );

        String errorJson = "{"
                + "\"error_code\": \"INTERNAL_SERVER_ERROR\","
                + "\"error_type\": \"API_ERROR\""
                + "}";
        ResponseBody errorBody = ResponseBody.create(errorJson, MediaType.parse("application/json"));

        when(plaidClient.itemPublicTokenExchange(any(ItemPublicTokenExchangeRequest.class)))
                .thenReturn(mockExchangeTokenCall);
        when(mockExchangeTokenCall.execute())
                .thenReturn(Response.error(500, errorBody));

        PlaidClientException exception = assertThrows(PlaidClientException.class, () -> {
            linkService.exchangeToken(requestDto, principal);
        });

        assertEquals("An error occurred in Plaid", exception.getMessage());
        assertEquals(HttpStatus.BAD_GATEWAY, exception.getHttpStatus());
    }

    @Test
    void exchangeToken_NetworkError() throws IOException {
        ExchangeTokenRequestDto requestDto = new ExchangeTokenRequestDto(
                "token",
                Collections.emptyList(),
                "ins_123",
                "Bank"
        );

        when(plaidClient.itemPublicTokenExchange(any())).thenReturn(mockExchangeTokenCall);
        when(mockExchangeTokenCall.execute()).thenThrow(new IOException("Timeout"));

        PlaidClientException exception = assertThrows(PlaidClientException.class, () -> {
            linkService.exchangeToken(requestDto, principal);
        });

        assertEquals("Unable to connect to banking provider.", exception.getMessage());
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exception.getHttpStatus());
    }
}
