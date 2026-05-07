package com.batu.ai_assistant.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.execution.ToolExecutionException;

import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;

class SpringAIConfigurationTest {

    @Test
    void toolExecutionExceptionProcessor_whenFeignFails_shouldHideBodyAndExposeStatus() {
        ToolExecutionException exception = new ToolExecutionException(
                toolDefinition("create_budget"),
                feignException(502));

        String response = new SpringAIConfiguration().toolExecutionExceptionProcessor().process(exception);

        assertThat(response).contains("\"success\":false");
        assertThat(response).contains("\"errorType\":\"BadGateway\"");
        assertThat(response).contains("\"message\":\"The downstream service returned HTTP 502.\"");
        assertThat(response).contains("\"tool\":\"create_budget\"");
    }

    @Test
    void toolExecutionExceptionProcessor_whenIllegalArgumentFails_shouldReturnSafeInvalidInputMessage() {
        ToolExecutionException exception = new ToolExecutionException(
                toolDefinition("update_budget"),
                new IllegalArgumentException("internal details"));

        String response = new SpringAIConfiguration().toolExecutionExceptionProcessor().process(exception);

        assertThat(response).contains("\"errorType\":\"IllegalArgumentException\"");
        assertThat(response).contains("\"message\":\"The tool received invalid input.\"");
        assertThat(response).contains("\"tool\":\"update_budget\"");
    }

    @Test
    void toolExecutionExceptionProcessor_whenMessageContainsQuotes_shouldJsonEscapeMessage() {
        ToolExecutionException exception = new ToolExecutionException(
                null,
                new RuntimeException("bad \"quoted\" value"));

        String response = new SpringAIConfiguration().toolExecutionExceptionProcessor().process(exception);

        assertThat(response).contains("\"message\":\"bad \\\"quoted\\\" value\"");
        assertThat(response).contains("\"tool\":\"unknown\"");
    }

    private static ToolDefinition toolDefinition(String name) {
        return ToolDefinition.builder()
                .name(name)
                .description("test tool")
                .inputSchema("{}")
                .build();
    }

    private static FeignException feignException(int status) {
        Request request = Request.create(
                Request.HttpMethod.GET,
                "http://downstream.example/test",
                java.util.Map.of(),
                null,
                new RequestTemplate());
        return FeignException.errorStatus("GET /test", feign.Response.builder()
                .status(status)
                .reason("Bad Gateway")
                .request(request)
                .build());
    }
}
