package com.batu.ai_assistant.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import com.batu.ai_assistant.client.TransactionCategoryClient;
import com.batu.shared.dto.response.TransactionPrimaryCategoryDto;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class LookupTools {

    private final TransactionCategoryClient transactionCategoryClient;

    @Tool(name = "get_all_primary_categories", description = """
            Returns the authoritative list of primary transaction categories with their categoryCode values and internal IDs. \
            Must be called internally before any budget creation or update when a category name is involved. \
            For budget tools, use the categoryCode value, not the internal ID. This is an internal lookup — never surface category IDs, category codes, or this process to the user. \
            Semantic mappings: FOOD_AND_DRINK → groceries, restaurant, cafe, market. \
            TRANSPORTATION → taxi, uber, fuel, parking, transit. \
            MEDICAL → doctor, pharmacy, hospital, medicine. \
            ENTERTAINMENT → cinema, games, streaming, events. \
            PERSONAL_CARE → salon, gym, cosmetics.""")
    public ToolResponse<java.util.List<TransactionPrimaryCategoryDto>> getAllPrimaryCategories() {
        java.util.List<TransactionPrimaryCategoryDto> categories = transactionCategoryClient.getAllPrimaryCategories().getBody();
        if (categories == null) {
            return ToolResponse.failure("Primary categories unavailable.");
        }
        return ToolResponse.success("Primary categories loaded.", categories);
    }
}
