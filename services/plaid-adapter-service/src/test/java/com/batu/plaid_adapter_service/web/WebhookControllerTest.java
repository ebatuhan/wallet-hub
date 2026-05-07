package com.batu.plaid_adapter_service.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.batu.plaid_adapter_service.config.KeycloakConfiguration;
import com.batu.plaid_adapter_service.controller.WebhookController;
import com.batu.plaid_adapter_service.dto.PlaidWebhookDto;
import com.batu.plaid_adapter_service.factory.WebhookFactory;
import com.batu.shared.error.CommonApplicationErrorAdvice;

import static org.assertj.core.api.Assertions.assertThat;

@WebMvcTest(
        controllers = WebhookController.class,
        properties = {
                "spring.cloud.config.enabled=false",
                "spring.config.import=",
                "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost/jwks",
                "spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost/issuer"
        })
@Import({ KeycloakConfiguration.class, CommonApplicationErrorAdvice.class })
class WebhookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WebhookFactory webhookFactory;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void handleWebhook_whenPayloadIsValidAndUnauthenticated_shouldExecuteFactoryAndReturnOk() throws Exception {
        ArgumentCaptor<PlaidWebhookDto> payloadCaptor = ArgumentCaptor.forClass(PlaidWebhookDto.class);

        mockMvc.perform(post("/api/plaid/webhook")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "webhook_type": "TRANSACTIONS",
                          "webhook_code": "SYNC_UPDATES_AVAILABLE",
                          "item_id": "item-1",
                          "new_transactions": 2,
                          "removed_transactions": 1
                        }
                        """))
                .andExpect(status().isOk());

        verify(webhookFactory).execute(payloadCaptor.capture());
        PlaidWebhookDto payload = payloadCaptor.getValue();
        assertThat(payload.webhookType()).isEqualTo("TRANSACTIONS");
        assertThat(payload.webhookCode()).isEqualTo("SYNC_UPDATES_AVAILABLE");
        assertThat(payload.itemId()).isEqualTo("item-1");
        assertThat(payload.newTransactions()).isEqualTo(2);
        assertThat(payload.removedTransactions()).isEqualTo(1);
    }

    @Test
    void handleWebhook_whenPayloadContainsError_shouldDeserializeErrorContract() throws Exception {
        ArgumentCaptor<PlaidWebhookDto> payloadCaptor = ArgumentCaptor.forClass(PlaidWebhookDto.class);

        mockMvc.perform(post("/api/plaid/webhook")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "webhook_type": "ITEM",
                          "webhook_code": "ERROR",
                          "item_id": "item-1",
                          "error": {
                            "error_code": "ITEM_LOGIN_REQUIRED",
                            "error_message": "login required"
                          }
                        }
                        """))
                .andExpect(status().isOk());

        verify(webhookFactory).execute(payloadCaptor.capture());
        assertThat(payloadCaptor.getValue().error().errorCode()).isEqualTo("ITEM_LOGIN_REQUIRED");
        assertThat(payloadCaptor.getValue().error().errorMessage()).isEqualTo("login required");
    }

    @Test
    void handleWebhook_whenRequestBodyIsMissing_shouldReturnBadRequestAndNotCallFactory() throws Exception {
        mockMvc.perform(post("/api/plaid/webhook"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(webhookFactory);
    }
}
