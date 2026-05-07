package com.batu.plaid_adapter_service.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import com.batu.plaid_adapter_service.config.KeycloakConfiguration;
import com.batu.plaid_adapter_service.controller.PlaidAdapterController;
import com.batu.plaid_adapter_service.entity.Connection;
import com.batu.plaid_adapter_service.mapper.ConnectionMapper;
import com.batu.plaid_adapter_service.service.ConnectionService;
import com.batu.plaid_adapter_service.service.PlaidIntegrationService;
import com.batu.shared.dto.request.ConnectionUpdateRequestDto;
import com.batu.shared.dto.request.ExchangeTokenRequestDto;
import com.batu.shared.dto.request.LinkTokenRequestDto;
import com.batu.shared.dto.response.ConnectionResponseDto;
import com.batu.shared.dto.response.ExchangeTokenResponseDto;
import com.batu.shared.dto.response.LinkTokenResponseDto;
import com.batu.shared.error.CommonApplicationErrorAdvice;

@WebMvcTest(
        controllers = PlaidAdapterController.class,
        properties = {
                "spring.cloud.config.enabled=false",
                "spring.config.import=",
                "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost/jwks",
                "spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost/issuer"
        })
@Import({ KeycloakConfiguration.class, CommonApplicationErrorAdvice.class })
class PlaidAdapterControllerTest {

    private static final UUID USER_ID = UUID.fromString("81000000-0000-0000-0000-000000000001");
    private static final UUID CONNECTION_ID = UUID.fromString("81000000-0000-0000-0000-000000000002");
    private static final Instant LAST_SYNCED_AT = Instant.parse("2026-05-07T08:15:30Z");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PlaidIntegrationService plaidIntegrationService;

    @MockitoBean
    private ConnectionService connectionService;

    @MockitoBean
    private ConnectionMapper connectionMapper;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void createLinkToken_whenRequestBodyProvided_shouldReturnLinkTokenAndUseAuthenticatedUser() throws Exception {
        ArgumentCaptor<LinkTokenRequestDto> requestCaptor = ArgumentCaptor.forClass(LinkTokenRequestDto.class);
        when(plaidIntegrationService.createLinkToken(any(LinkTokenRequestDto.class), eq(USER_ID)))
                .thenReturn(new LinkTokenResponseDto("link-token"));

        mockMvc.perform(post("/plaid/link-token")
                .with(jwt().jwt(jwt -> jwt.subject(USER_ID.toString())))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "country": "CA"
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.linkToken").value("link-token"));

        verify(plaidIntegrationService).createLinkToken(requestCaptor.capture(), eq(USER_ID));
        assertThat(requestCaptor.getValue().getCountry()).isEqualTo("CA");
    }

    @Test
    void createLinkToken_whenRequestBodyIsMissing_shouldUseDefaultUsRequest() throws Exception {
        ArgumentCaptor<LinkTokenRequestDto> requestCaptor = ArgumentCaptor.forClass(LinkTokenRequestDto.class);
        when(plaidIntegrationService.createLinkToken(any(LinkTokenRequestDto.class), eq(USER_ID)))
                .thenReturn(new LinkTokenResponseDto("link-token"));

        mockMvc.perform(post("/plaid/link-token")
                .with(jwt().jwt(jwt -> jwt.subject(USER_ID.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.linkToken").value("link-token"));

        verify(plaidIntegrationService).createLinkToken(requestCaptor.capture(), eq(USER_ID));
        assertThat(requestCaptor.getValue().getCountry()).isEqualTo("US");
    }

    @Test
    void exchangeToken_whenRequestIsValid_shouldReturnExchangeResponseAndUseAuthenticatedUser() throws Exception {
        when(plaidIntegrationService.exchangeLinkToken(any(ExchangeTokenRequestDto.class), eq(USER_ID)))
                .thenReturn(new ExchangeTokenResponseDto(CONNECTION_ID, "ins-1", "Test Bank"));

        mockMvc.perform(post("/plaid/exchange")
                .with(jwt().jwt(jwt -> jwt.subject(USER_ID.toString())))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "publicToken": "public-token",
                          "accountIds": ["account-1"],
                          "accounts": [{"id":"account-1","name":"Checking","mask":"0000","subType":"checking"}],
                          "institutionId": "ins-1",
                          "institutionName": "Test Bank"
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.connectionId").value(CONNECTION_ID.toString()))
                .andExpect(jsonPath("$.institutionId").value("ins-1"))
                .andExpect(jsonPath("$.institutionName").value("Test Bank"));

        verify(plaidIntegrationService).exchangeLinkToken(any(ExchangeTokenRequestDto.class), eq(USER_ID));
    }

    @Test
    void mockToken_whenAuthenticated_shouldReturnExchangeResponse() throws Exception {
        when(plaidIntegrationService.mockToken(USER_ID))
                .thenReturn(new ExchangeTokenResponseDto(CONNECTION_ID, "ins-1", "Sandbox Bank"));

        mockMvc.perform(post("/plaid/mock")
                .with(jwt().jwt(jwt -> jwt.subject(USER_ID.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.connectionId").value(CONNECTION_ID.toString()))
                .andExpect(jsonPath("$.institutionName").value("Sandbox Bank"));
    }

    @Test
    void listConnections_whenAuthenticated_shouldReturnMappedConnectionsForUser() throws Exception {
        Connection connection = connection(true);
        when(connectionService.readAllByUserId(USER_ID)).thenReturn(List.of(connection));
        when(connectionMapper.toResponse(connection)).thenReturn(connectionResponse(true));

        mockMvc.perform(get("/plaid/connections")
                .with(jwt().jwt(jwt -> jwt.subject(USER_ID.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].connectionId").value(CONNECTION_ID.toString()))
                .andExpect(jsonPath("$[0].provider").value("PLAID"))
                .andExpect(jsonPath("$[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$[0].supportsRefresh").value(true))
                .andExpect(jsonPath("$[0].supportsDisconnect").value(true));

        verify(connectionService).readAllByUserId(USER_ID);
    }

    @Test
    void getConnection_whenConnectionBelongsToUser_shouldReturnConnection() throws Exception {
        Connection connection = connection(true);
        when(connectionService.readByIdAndUserId(CONNECTION_ID, USER_ID)).thenReturn(connection);
        when(connectionMapper.toResponse(connection)).thenReturn(connectionResponse(true));

        mockMvc.perform(get("/plaid/connections/{connectionId}", CONNECTION_ID)
                .with(jwt().jwt(jwt -> jwt.subject(USER_ID.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.connectionId").value(CONNECTION_ID.toString()))
                .andExpect(jsonPath("$.displayName").value("Test Bank"));
    }

    @Test
    void updateConnection_whenConnectionBelongsToUser_shouldApplyRequestThenPersistAndReturnConnection() throws Exception {
        Connection connection = connection(true);
        Connection updatedConnection = connection(true);
        updatedConnection.setDisplayName("Updated Bank");
        when(connectionService.readByIdAndUserId(CONNECTION_ID, USER_ID)).thenReturn(connection);
        when(connectionService.updateById(eq(CONNECTION_ID), any(Connection.class))).thenReturn(updatedConnection);
        when(connectionMapper.toResponse(updatedConnection)).thenReturn(new ConnectionResponseDto(
                CONNECTION_ID, "PLAID", "Plaid", "ins-1", "Test Bank", "Updated Bank", "ACTIVE",
                LAST_SYNCED_AT, true, false, true));

        mockMvc.perform(patch("/plaid/connections/{connectionId}", CONNECTION_ID)
                .with(jwt().jwt(jwt -> jwt.subject(USER_ID.toString())))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "displayName": "Updated Bank"
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Updated Bank"));

        verify(connectionService).readByIdAndUserId(CONNECTION_ID, USER_ID);
        verify(connectionMapper).updateConnectionFromRequest(any(ConnectionUpdateRequestDto.class), eq(connection));
        verify(connectionService).updateById(CONNECTION_ID, connection);
    }

    @Test
    void refreshConnection_whenConnectionBelongsToUser_shouldCheckOwnershipBeforeSyncing() throws Exception {
        Connection connection = connection(true);
        when(connectionService.readByIdAndUserId(CONNECTION_ID, USER_ID)).thenReturn(connection);

        mockMvc.perform(post("/plaid/connections/{connectionId}/refresh", CONNECTION_ID)
                .with(jwt().jwt(jwt -> jwt.subject(USER_ID.toString()))))
                .andExpect(status().isNoContent());

        verify(connectionService).readByIdAndUserId(CONNECTION_ID, USER_ID);
        verify(plaidIntegrationService).syncConnection(CONNECTION_ID);
    }

    @Test
    void removeConnection_whenConnectionBelongsToUser_shouldCheckOwnershipBeforeRemoving() throws Exception {
        Connection connection = connection(true);
        when(connectionService.readByIdAndUserId(CONNECTION_ID, USER_ID)).thenReturn(connection);

        mockMvc.perform(delete("/plaid/connections/{connectionId}", CONNECTION_ID)
                .with(jwt().jwt(jwt -> jwt.subject(USER_ID.toString()))))
                .andExpect(status().isNoContent());

        verify(connectionService).readByIdAndUserId(CONNECTION_ID, USER_ID);
        verify(plaidIntegrationService).removeConnection(CONNECTION_ID, "USER_REQUESTED_REMOVAL");
    }

    @ParameterizedTest
    @ValueSource(strings = { "refresh", "remove", "update" })
    void mutatingConnectionEndpoints_whenConnectionDoesNotBelongToUser_shouldStopBeforeMutation(String operation)
            throws Exception {
        when(connectionService.readByIdAndUserId(CONNECTION_ID, USER_ID))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Connection not found"));

        if ("refresh".equals(operation)) {
            mockMvc.perform(post("/plaid/connections/{connectionId}/refresh", CONNECTION_ID)
                    .with(jwt().jwt(jwt -> jwt.subject(USER_ID.toString()))))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.detail").value("Connection not found"));
        } else if ("remove".equals(operation)) {
            mockMvc.perform(delete("/plaid/connections/{connectionId}", CONNECTION_ID)
                    .with(jwt().jwt(jwt -> jwt.subject(USER_ID.toString()))))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.detail").value("Connection not found"));
        } else {
            mockMvc.perform(patch("/plaid/connections/{connectionId}", CONNECTION_ID)
                    .with(jwt().jwt(jwt -> jwt.subject(USER_ID.toString())))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"displayName\":\"Updated\"}"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.detail").value("Connection not found"));
        }

        verify(plaidIntegrationService, never()).syncConnection(any());
        verify(plaidIntegrationService, never()).removeConnection(any(), any());
        verify(connectionService, never()).updateById(any(), any());
    }

    @Test
    void createLinkToken_whenUnauthenticated_shouldReturnUnauthorized() throws Exception {
        mockMvc.perform(post("/plaid/link-token"))
                .andExpect(status().isUnauthorized());

        verify(plaidIntegrationService, never()).createLinkToken(any(), any());
    }

    @Test
    void listConnections_whenJwtSubjectIsMalformed_shouldReturnBadRequestAndNotCallService() throws Exception {
        mockMvc.perform(get("/plaid/connections")
                .with(jwt().jwt(jwt -> jwt.subject("not-a-uuid"))))
                .andExpect(status().isBadRequest());

        verify(connectionService, never()).readAllByUserId(any());
    }

    private Connection connection(boolean active) {
        Connection connection = new Connection(USER_ID, "item-1", "access-token", "ins-1", "Test Bank");
        ReflectionTestUtils.setField(connection, "connectionId", CONNECTION_ID);
        connection.setActive(active);
        connection.setLastSyncedAt(LAST_SYNCED_AT);
        return connection;
    }

    private ConnectionResponseDto connectionResponse(boolean active) {
        return new ConnectionResponseDto(
                CONNECTION_ID,
                "PLAID",
                "Plaid",
                "ins-1",
                "Test Bank",
                "Test Bank",
                active ? "ACTIVE" : "INACTIVE",
                LAST_SYNCED_AT,
                active,
                false,
                active);
    }
}
