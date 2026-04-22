package com.batu.ai_assistant.exception;

import org.springframework.http.HttpStatus;

import com.batu.shared.exception.AbstractApplicationException;

public class ModelNotConfiguredException extends AbstractApplicationException {
    public ModelNotConfiguredException() {
        super("MODEL_NOT_CONFIGURED",
                "AI model is not configured yet. Add a Spring AI chat model provider later.",
                HttpStatus.SERVICE_UNAVAILABLE);
    }
}
