package com.batu.plaid_adapter_service.unit.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.util.ReflectionTestUtils;

import com.batu.plaid_adapter_service.dto.LinkTokenRequestDto;
import com.batu.plaid_adapter_service.dto.LinkTokenResponseDto;
import com.batu.plaid_adapter_service.exception.PlaidClientException;
import com.batu.plaid_adapter_service.service.impl.LinkServiceImpl;
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
    private Call<LinkTokenCreateResponse> mockCall;

    @Mock
    private Jwt principal;

    @InjectMocks
    private LinkServiceImpl linkService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(linkService, "webhookUrl", "https://test-webhook.com");

        when(principal.getSubject())
                .thenReturn("test-user");
    }

    @Test
    void createLinkToken_sucesfullResponse_returnsDto() throws IOException {

        String expectedLinkToken = "test-link-token";

        LinkTokenCreateResponse sucessResponse = new LinkTokenCreateResponse();

        sucessResponse.setLinkToken(expectedLinkToken);

        when(plaidClient.linkTokenCreate(any(LinkTokenCreateRequest.class)))
                .thenReturn(mockCall);

        when(mockCall.execute())
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

        when(plaidClient.linkTokenCreate(any())).thenReturn(mockCall);

        when(mockCall.execute())
                .thenReturn(Response.error(400, errorBody));

        PlaidClientException exception = assertThrows(PlaidClientException.class, () -> {
            linkService.createLinkToken(new LinkTokenRequestDto(), principal);
        });

        assertEquals("Something went wrong with the bank", exception.getMessage());
        assertEquals(HttpStatus.BAD_GATEWAY, exception.getHttpStatus());
    }

    @Test
    void createLinkToken_NetworkError() throws IOException {
        when(principal.getSubject()).thenReturn("user-123");

        when(plaidClient.linkTokenCreate(any())).thenReturn(mockCall);
        when(mockCall.execute()).thenThrow(new IOException("Network failure"));

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
        when(plaidClient.linkTokenCreate(any())).thenReturn(mockCall);

        when(mockCall.execute())
                .thenReturn(Response.error(400, errorBody));

        PlaidClientException exception = assertThrows(PlaidClientException.class, () -> {
            linkService.createLinkToken(new LinkTokenRequestDto(), principal);
        });

        assertEquals("An error occurred in Plaid", exception.getMessage());
        assertEquals(HttpStatus.BAD_GATEWAY, exception.getHttpStatus());

    }
}
