package com.batu.ai_assistant.tools;

public record ToolResponse<T>(boolean success, String message, T data) {

    public static <T> ToolResponse<T> success(String message, T data) {
        return new ToolResponse<>(true, message, data);
    }

    public static <T> ToolResponse<T> failure(String message) {
        return new ToolResponse<>(false, message, null);
    }
}
