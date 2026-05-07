package com.batu.ai_assistant.util;

import org.springframework.stereotype.Component;

@Component
public class AssistantResponseSanitizer {

    public String sanitize(String response) {
        if (response == null) {
            return null;
        }

        return response
                .replaceAll("\\(ID:\\s*[0-9a-fA-F-]{36}\\)", "")
                .replaceAll("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}", "")
                .replaceAll("\\s{2,}", " ")
                .replace(" .", ".")
                .trim();
    }
}
