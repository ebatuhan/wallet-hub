package com.batu.transaction_service.web;

import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.batu.shared.dto.response.TransactionPrimaryCategoryDto;
import com.batu.shared.error.CommonApplicationErrorAdvice;
import com.batu.transaction_service.TransactionCategoryController;
import com.batu.transaction_service.config.KeycloakConfiguration;
import com.batu.transaction_service.entity.TransactionPrimaryCategory;
import com.batu.transaction_service.service.PrimaryCategoryService;

@WebMvcTest(
        controllers = TransactionCategoryController.class,
        properties = {
                "spring.cloud.config.enabled=false",
                "spring.config.import=",
                "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost/jwks",
                "spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost/issuer"
        })
@Import({ KeycloakConfiguration.class, CommonApplicationErrorAdvice.class })
class TransactionCategoryControllerTest {

    private static final UUID PRIMARY_CATEGORY_ID = UUID.fromString("c5000000-0000-0000-0000-000000000001");
    private static final UUID OTHER_PRIMARY_CATEGORY_ID = UUID.fromString("c5000000-0000-0000-0000-000000000002");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PrimaryCategoryService primaryCategoryService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void getAllPrimaryCategories_whenUnauthenticated_shouldReturnCategoriesBecauseRouteIsPublic() throws Exception {
        when(primaryCategoryService.getAll()).thenReturn(List.of(categoryDto(PRIMARY_CATEGORY_ID, "FOOD_AND_DRINK")));

        mockMvc.perform(get("/transactions/categories/primary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].transactionPrimaryCategoryId").value(PRIMARY_CATEGORY_ID.toString()))
                .andExpect(jsonPath("$[0].categoryCode").value("FOOD_AND_DRINK"));

        verify(primaryCategoryService).getAll();
    }

    @Test
    void getPrimaryCategoryById_whenIdIsValid_shouldReturnCategory() throws Exception {
        when(primaryCategoryService.getById(PRIMARY_CATEGORY_ID))
                .thenReturn(new TransactionPrimaryCategory("FOOD_AND_DRINK", "Food & Drink", "food.svg"));

        mockMvc.perform(get("/transactions/categories/primary/{id}", PRIMARY_CATEGORY_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categoryCode").value("FOOD_AND_DRINK"))
                .andExpect(jsonPath("$.displayName").value("Food & Drink"));

        verify(primaryCategoryService).getById(PRIMARY_CATEGORY_ID);
    }

    @Test
    void getPrimaryCategoryById_whenIdIsInvalid_shouldReturnBadRequestAndNotCallService() throws Exception {
        mockMvc.perform(get("/transactions/categories/primary/{id}", "not-a-uuid"))
                .andExpect(status().isBadRequest());

        verify(primaryCategoryService, never()).getById(org.mockito.ArgumentMatchers.any(UUID.class));
    }

    @Test
    void getPrimaryCategoriesByIds_whenRequestIsValid_shouldReturnCategories() throws Exception {
        when(primaryCategoryService.getByIds(Set.of(PRIMARY_CATEGORY_ID, OTHER_PRIMARY_CATEGORY_ID))).thenReturn(List.of(
                categoryDto(PRIMARY_CATEGORY_ID, "FOOD_AND_DRINK"),
                categoryDto(OTHER_PRIMARY_CATEGORY_ID, "TRAVEL")));

        mockMvc.perform(post("/transactions/categories/primary/by-ids")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "ids": [
                            "c5000000-0000-0000-0000-000000000001",
                            "c5000000-0000-0000-0000-000000000002"
                          ]
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].categoryCode").value("FOOD_AND_DRINK"))
                .andExpect(jsonPath("$[1].categoryCode").value("TRAVEL"));

        verify(primaryCategoryService).getByIds(Set.of(PRIMARY_CATEGORY_ID, OTHER_PRIMARY_CATEGORY_ID));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{ \"ids\": [] }",
            "{}"
    })
    void getPrimaryCategoriesByIds_whenIdsAreMissingOrEmpty_shouldReturnValidationProblemAndNotCallService(String requestBody) throws Exception {
        mockMvc.perform(post("/transactions/categories/primary/by-ids")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Request validation failed"))
                .andExpect(jsonPath("$.errors.ids").value("At least one category id is required"));

        verify(primaryCategoryService, never()).getByIds(anySet());
    }

    @Test
    void getPrimaryCategoriesByIds_whenIdsContainNull_shouldReturnValidationProblemAndNotCallService() throws Exception {
        mockMvc.perform(post("/transactions/categories/primary/by-ids")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "ids": [null]
                        }
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Request validation failed"));

        verify(primaryCategoryService, never()).getByIds(anySet());
    }

    @Test
    void getPrimaryCategoriesByIds_whenJsonIsMalformed_shouldReturnBadRequestAndNotCallService() throws Exception {
        mockMvc.perform(post("/transactions/categories/primary/by-ids")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{"))
                .andExpect(status().isBadRequest());

        verify(primaryCategoryService, never()).getByIds(anySet());
    }

    private TransactionPrimaryCategoryDto categoryDto(UUID id, String code) {
        return new TransactionPrimaryCategoryDto(id, code, code.replace('_', ' '), code.toLowerCase() + ".svg");
    }
}
