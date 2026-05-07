package com.batu.plaid_adapter_service.unit.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.plaid.client.model.AccountsGetRequest;
import com.plaid.client.model.AccountsGetResponse;
import com.batu.plaid_adapter_service.client.PlaidClientWrapper;
import com.batu.plaid_adapter_service.exception.PlaidRetryableException;
import com.batu.plaid_adapter_service.factory.PlaidErrorHandlerFactory;
import com.plaid.client.model.ItemPublicTokenExchangeRequest;
import com.plaid.client.model.ItemPublicTokenExchangeResponse;
import com.plaid.client.model.ItemRemoveRequest;
import com.plaid.client.model.ItemRemoveResponse;
import com.plaid.client.model.LinkTokenCreateRequest;
import com.plaid.client.model.LinkTokenCreateResponse;
import com.plaid.client.model.PlaidError;
import com.plaid.client.model.SandboxPublicTokenCreateRequest;
import com.plaid.client.model.SandboxPublicTokenCreateResponse;
import com.plaid.client.model.TransactionsSyncRequest;
import com.plaid.client.model.TransactionsSyncResponse;
import com.plaid.client.request.PlaidApi;

import okhttp3.MediaType;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;

@ExtendWith(MockitoExtension.class)
class PlaidClientWrapperTest {

    @Mock
    private PlaidApi plaidApi;

    @Mock
    private PlaidErrorHandlerFactory plaidErrorHandlerFactory;

    private PlaidClientWrapper wrapper;

    @BeforeEach
    void setUp() {
        wrapper = new PlaidClientWrapper(plaidApi, plaidErrorHandlerFactory);
    }

    @Test
    void exchangePublicToken_whenPlaidReturnsSuccessfulBody_shouldDelegateToExchangeEndpoint() throws Exception {
        ItemPublicTokenExchangeResponse plaidResponse = new ItemPublicTokenExchangeResponse().itemId("item-1");
        Call<ItemPublicTokenExchangeResponse> call = call(Response.success(plaidResponse));
        ItemPublicTokenExchangeRequest request = new ItemPublicTokenExchangeRequest().publicToken("public-token");
        when(plaidApi.itemPublicTokenExchange(request)).thenReturn(call);

        ItemPublicTokenExchangeResponse response = wrapper.exchangePublicToken(request);

        assertThat(response).isSameAs(plaidResponse);
        verify(plaidApi).itemPublicTokenExchange(request);
    }

    @Test
    void createSandboxToken_whenPlaidReturnsSuccessfulBody_shouldDelegateToSandboxEndpoint() throws Exception {
        SandboxPublicTokenCreateResponse plaidResponse = new SandboxPublicTokenCreateResponse().publicToken("public-token");
        Call<SandboxPublicTokenCreateResponse> call = call(Response.success(plaidResponse));
        SandboxPublicTokenCreateRequest request = new SandboxPublicTokenCreateRequest().institutionId("ins-1");
        when(plaidApi.sandboxPublicTokenCreate(request)).thenReturn(call);

        SandboxPublicTokenCreateResponse response = wrapper.createSandboxToken(request);

        assertThat(response).isSameAs(plaidResponse);
        verify(plaidApi).sandboxPublicTokenCreate(request);
    }

    @Test
    void syncTransactions_whenPlaidReturnsSuccessfulBody_shouldDelegateToTransactionsSyncEndpoint() throws Exception {
        TransactionsSyncResponse plaidResponse = new TransactionsSyncResponse().nextCursor("cursor-1");
        Call<TransactionsSyncResponse> call = call(Response.success(plaidResponse));
        TransactionsSyncRequest request = new TransactionsSyncRequest().accessToken("access-token");
        when(plaidApi.transactionsSync(request)).thenReturn(call);

        TransactionsSyncResponse response = wrapper.syncTransactions(request);

        assertThat(response).isSameAs(plaidResponse);
        verify(plaidApi).transactionsSync(request);
    }

    @Test
    void accountsGet_whenPlaidReturnsSuccessfulBody_shouldDelegateToAccountsGetEndpoint() throws Exception {
        AccountsGetResponse plaidResponse = new AccountsGetResponse().requestId("request-1");
        Call<AccountsGetResponse> call = call(Response.success(plaidResponse));
        AccountsGetRequest request = new AccountsGetRequest().accessToken("access-token");
        when(plaidApi.accountsGet(request)).thenReturn(call);

        AccountsGetResponse response = wrapper.accountsGet(request);

        assertThat(response).isSameAs(plaidResponse);
        verify(plaidApi).accountsGet(request);
    }

    @Test
    void createLinkToken_whenPlaidReturnsSuccessfulBody_shouldReturnBody() throws Exception {
        LinkTokenCreateResponse plaidResponse = new LinkTokenCreateResponse().linkToken("link-token");
        stubLinkToken(Response.success(plaidResponse));

        LinkTokenCreateResponse response = wrapper.createLinkToken(new LinkTokenCreateRequest());

        assertThat(response).isSameAs(plaidResponse);
        assertThat(response.getLinkToken()).isEqualTo("link-token");
    }

    @Test
    void removeItem_whenPlaidReturnsSuccessfulBody_shouldDelegateToItemRemoveEndpoint() throws Exception {
        ItemRemoveResponse plaidResponse = new ItemRemoveResponse().requestId("request-1");
        Call<ItemRemoveResponse> call = call(Response.success(plaidResponse));
        ItemRemoveRequest request = new ItemRemoveRequest().accessToken("access-token");
        when(plaidApi.itemRemove(request)).thenReturn(call);

        ItemRemoveResponse response = wrapper.removeItem(request);

        assertThat(response).isSameAs(plaidResponse);
        verify(plaidApi).itemRemove(request);
    }

    @Test
    void createLinkToken_whenSuccessfulResponseHasNoBody_shouldThrowBadGateway() throws Exception {
        stubLinkToken(Response.success(null));

        assertThatThrownBy(() -> wrapper.createLinkToken(new LinkTokenCreateRequest()))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception -> {
                    assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
                    assertThat(exception.getReason()).isEqualTo("Plaid returned empty response body");
                });
    }

    @Test
    void createLinkToken_whenPlaidReturnsValidError_shouldThrowFactoryException() throws Exception {
        RuntimeException expected = new PlaidRetryableException("rate limited", HttpStatus.TOO_MANY_REQUESTS);
        when(plaidErrorHandlerFactory.toException(any(PlaidError.class))).thenReturn(expected);
        stubLinkToken(errorResponse("""
                {
                  "error_code": "RATE_LIMIT_EXCEEDED",
                  "error_message": "rate limited"
                }
                """));

        assertThatThrownBy(() -> wrapper.createLinkToken(new LinkTokenCreateRequest())).isSameAs(expected);
        verify(plaidErrorHandlerFactory).toException(any(PlaidError.class));
    }

    @Test
    void createLinkToken_whenPlaidErrorBodyIsMissing_shouldThrowBadGateway() throws Exception {
        Response<LinkTokenCreateResponse> response = mock(Response.class);
        when(response.isSuccessful()).thenReturn(false);
        when(response.errorBody()).thenReturn(null);
        stubLinkToken(response);

        assertThatThrownBy(() -> wrapper.createLinkToken(new LinkTokenCreateRequest()))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception -> {
                    assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
                    assertThat(exception.getReason()).isEqualTo("Plaid returned empty error body");
                });
    }

    @Test
    void createLinkToken_whenPlaidErrorBodyIsMalformedJson_shouldThrowBadGateway() throws Exception {
        stubLinkToken(errorResponse("{"));

        assertThatThrownBy(() -> wrapper.createLinkToken(new LinkTokenCreateRequest()))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception -> {
                    assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
                    assertThat(exception.getReason()).isEqualTo("Plaid returned malformed error body");
                });
    }

    @ParameterizedTest
    @ValueSource(strings = { "{}", "{\"error_code\": null}", "{\"error_code\": \"\"}", "{\"error_code\": \"   \"}" })
    void createLinkToken_whenPlaidErrorBodyHasMissingBlankOrNullErrorCode_shouldThrowBadGateway(String body)
            throws Exception {
        stubLinkToken(errorResponse(body));

        assertThatThrownBy(() -> wrapper.createLinkToken(new LinkTokenCreateRequest()))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception -> {
                    assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
                    assertThat(exception.getReason()).isEqualTo("Plaid returned malformed error body");
                });
    }

    @Test
    void createLinkToken_whenPlaidCallFailsWithIoException_shouldThrowRetryableBadGateway() throws Exception {
        Call<LinkTokenCreateResponse> call = mock(Call.class);
        when(call.execute()).thenThrow(new IOException("network down"));
        when(plaidApi.linkTokenCreate(any(LinkTokenCreateRequest.class))).thenReturn(call);

        assertThatThrownBy(() -> wrapper.createLinkToken(new LinkTokenCreateRequest()))
                .isInstanceOfSatisfying(PlaidRetryableException.class, exception -> {
                    assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
                    assertThat(exception.getReason()).isEqualTo("Unable to connect to Plaid.");
                });
    }

    private void stubLinkToken(Response<LinkTokenCreateResponse> response) throws IOException {
        Call<LinkTokenCreateResponse> plaidCall = call(response);
        when(plaidApi.linkTokenCreate(any(LinkTokenCreateRequest.class))).thenReturn(plaidCall);
    }

    private Response<LinkTokenCreateResponse> errorResponse(String body) {
        return Response.error(400, ResponseBody.create(body, MediaType.parse("application/json")));
    }

    private <T> Call<T> call(Response<T> response) throws IOException {
        Call<T> call = mock(Call.class);
        when(call.execute()).thenReturn(response);
        return call;
    }
}
