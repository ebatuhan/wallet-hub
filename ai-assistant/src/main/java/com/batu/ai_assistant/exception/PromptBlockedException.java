package com.batu.ai_assistant.exception;

import org.springframework.http.HttpStatus;

import com.batu.ai_assistant.exception.AbstractApplicationException;

public class PromptBlockedException extends AbstractApplicationException {

    public PromptBlockedException(String message) {
        super("PROMPT_BLOCKED", message, HttpStatus.FORBIDDEN);
    }
}
