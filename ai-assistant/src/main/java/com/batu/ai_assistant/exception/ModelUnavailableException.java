package com.batu.ai_assistant.exception;

import org.springframework.http.HttpStatus;

public class ModelUnavailableException extends AbstractApplicationException {
    public ModelUnavailableException() {
        super("MODEL_UNAVAILABLE",
                "AI model is currently unavailable. Please try again later.",
                HttpStatus.SERVICE_UNAVAILABLE);
    }
}
