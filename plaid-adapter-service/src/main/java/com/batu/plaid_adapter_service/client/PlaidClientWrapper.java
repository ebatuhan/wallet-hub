package com.batu.plaid_adapter_service.client;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.batu.plaid_adapter_service.exception.PlaidClientException;
import com.batu.plaid_adapter_service.exception.PlaidRetryableException;
import com.batu.plaid_adapter_service.factory.PlaidErrorHandlerFactory;
import com.google.gson.Gson;
import com.plaid.client.model.AccountsGetRequest;
import com.plaid.client.model.AccountsGetResponse;
import com.plaid.client.model.ItemRemoveRequest;
import com.plaid.client.model.ItemRemoveResponse;
import com.plaid.client.model.ItemPublicTokenExchangeRequest;
import com.plaid.client.model.ItemPublicTokenExchangeResponse;
import com.plaid.client.model.LinkTokenCreateRequest;
import com.plaid.client.model.LinkTokenCreateResponse;
import com.plaid.client.model.PlaidError;
import com.plaid.client.model.SandboxPublicTokenCreateRequest;
import com.plaid.client.model.SandboxPublicTokenCreateResponse;
import com.plaid.client.model.TransactionsSyncRequest;
import com.plaid.client.model.TransactionsSyncResponse;
import com.plaid.client.request.PlaidApi;

import retrofit2.Response;

@Component
public class PlaidClientWrapper {

    private final PlaidApi plaidClient;
    private final PlaidErrorHandlerFactory plaidErrorHandlerFactory;
    private final Gson gson;

    public PlaidClientWrapper(PlaidApi plaidClient, PlaidErrorHandlerFactory plaidErrorHandlerFactory) {
        this.plaidClient = plaidClient;
        this.plaidErrorHandlerFactory = plaidErrorHandlerFactory;
        this.gson = new Gson();
    }

    public LinkTokenCreateResponse createLinkToken(LinkTokenCreateRequest request) {
        return executeRequest(() -> plaidClient.linkTokenCreate(request).execute());
    }

    public ItemPublicTokenExchangeResponse exchangePublicToken(ItemPublicTokenExchangeRequest request) {
        return executeRequest(() -> plaidClient.itemPublicTokenExchange(request).execute());
    }

    public SandboxPublicTokenCreateResponse createSandboxToken(SandboxPublicTokenCreateRequest request) {
        return executeRequest(() -> plaidClient.sandboxPublicTokenCreate(request).execute());
    }

    public TransactionsSyncResponse syncTransactions(TransactionsSyncRequest request) {
        return executeRequest(() -> plaidClient.transactionsSync(request).execute());
    }

    public AccountsGetResponse accountsGet(AccountsGetRequest request) {
        return executeRequest(() -> plaidClient.accountsGet(request).execute());
    }

    public ItemRemoveResponse removeItem(ItemRemoveRequest request) {
        return executeRequest(() -> plaidClient.itemRemove(request).execute());
    }

    private <T> T executeRequest(PlaidRequestSupplier<T> supplier) {
        try {
            Response<T> response = supplier.get();
            return handleResponse(response);
        } catch (IOException e) {
            throw new PlaidRetryableException("Unable to connect to Plaid.", HttpStatus.BAD_GATEWAY);
        }
    }

    private <T> T handleResponse(Response<T> response) throws IOException {
        if (response.isSuccessful() && response.body() != null) {
            return response.body();
        }

        var errorBody = response.errorBody();
        if (errorBody == null) {
            throw new PlaidClientException("Plaid returned empty error body", HttpStatus.BAD_GATEWAY);
        }

        PlaidError plaidError = gson.fromJson(errorBody.string(), PlaidError.class);

        plaidErrorHandlerFactory.execute(plaidError);

        return null; //TODO ?????
    }

    @FunctionalInterface
    private interface PlaidRequestSupplier<T> {
        Response<T> get() throws IOException;
    }
}
