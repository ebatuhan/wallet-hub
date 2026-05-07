package com.batu.ai_assistant.unit.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.ai.ollama.api.ThinkOption;
import org.springframework.ai.retry.TransientAiException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.server.ResponseStatusException;

import com.batu.ai_assistant.dto.ChatRequestDTO;
import com.batu.ai_assistant.service.impl.AIAssistantServiceImpl;
import com.batu.ai_assistant.util.AssistantResponseSanitizer;

@ExtendWith(MockitoExtension.class)
class AIAssistantServiceImplTest {

    private static final UUID USER_ID = UUID.fromString("91000000-0000-0000-0000-000000000001");

    @Mock
    private ChatClient chatClient;

    @Mock
    private ChatClient.ChatClientRequestSpec requestSpec;

    @Mock
    private ChatClient.CallResponseSpec callResponseSpec;

    @Test
    void chat_whenModelIsMissing_shouldReturnServiceUnavailableAndNotCallChatClient() {
        AIAssistantServiceImpl service = serviceWithModel(" ", "ENABLED");

        assertThatThrownBy(() -> service.chat(new ChatRequestDTO("How am I doing?"), jwt()))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception -> {
                    assertThat(exception.getStatusCode().value()).isEqualTo(503);
                    assertThat(exception.getReason()).contains("AI model is not configured");
                });

        verify(chatClient, never()).prompt();
    }

    @Test
    void chat_whenModelResponds_shouldPassConversationIdApplyOptionsAndSanitizeUuidLeakage() {
        stubChatClientRequestChain();
        AIAssistantServiceImpl service = serviceWithModel("qwen3", "ENABLED");
        when(callResponseSpec.content()).thenReturn(
                "Food budget is ready (ID: 91000000-0000-0000-0000-000000000099) for category 91000000-0000-0000-0000-000000000098.");
        ArgumentCaptor<Consumer<ChatClient.AdvisorSpec>> advisorCaptor = ArgumentCaptor.captor();
        ArgumentCaptor<OllamaChatOptions> optionsCaptor = ArgumentCaptor.forClass(OllamaChatOptions.class);

        var response = service.chat(new ChatRequestDTO("Create my food budget"), jwt());

        assertThat(response.message()).isEqualTo("Food budget is ready for category.");
        verify(requestSpec).user("Create my food budget");
        verify(requestSpec).advisors(advisorCaptor.capture());
        ChatClient.AdvisorSpec advisorSpec = org.mockito.Mockito.mock(ChatClient.AdvisorSpec.class);
        advisorCaptor.getValue().accept(advisorSpec);
        verify(advisorSpec).param(ChatMemory.CONVERSATION_ID, USER_ID.toString());
        verify(requestSpec).options(optionsCaptor.capture());
        assertThat(optionsCaptor.getValue().getModel()).isEqualTo("qwen3");
        assertThat(optionsCaptor.getValue().getKeepAlive()).isEqualTo("30m");
        assertThat(optionsCaptor.getValue().getNumCtx()).isEqualTo(8192);
        assertThat(optionsCaptor.getValue().getNumPredict()).isEqualTo(2048);
        assertThat(optionsCaptor.getValue().getTemperature()).isEqualTo(0.2);
        assertThat(optionsCaptor.getValue().getSeed()).isEqualTo(7);
    }

    @ParameterizedTest
    @MethodSource("thinkingModes")
    void chat_whenThinkingModeConfigured_shouldApplyExpectedOllamaOption(String configuredMode, Object expectedJsonValue) {
        stubChatClientRequestChain();
        AIAssistantServiceImpl service = serviceWithModel("qwen3", configuredMode);
        when(callResponseSpec.content()).thenReturn("ok");
        ArgumentCaptor<OllamaChatOptions> optionsCaptor = ArgumentCaptor.forClass(OllamaChatOptions.class);

        service.chat(new ChatRequestDTO("Hello"), jwt());

        verify(requestSpec).options(optionsCaptor.capture());
        ThinkOption thinkOption = optionsCaptor.getValue().getThinkOption();
        assertThat(thinkOption.toJsonValue()).isEqualTo(expectedJsonValue);
    }

    @ParameterizedTest
    @MethodSource("aiUnavailableExceptions")
    void chat_whenAiBoundaryIsUnavailable_shouldReturnServiceUnavailable(RuntimeException failure) {
        stubChatClientRequestChain();
        AIAssistantServiceImpl service = serviceWithModel("qwen3", "ENABLED");
        when(requestSpec.call()).thenThrow(failure);

        assertThatThrownBy(() -> service.chat(new ChatRequestDTO("Hello"), jwt()))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception -> {
                    assertThat(exception.getStatusCode().value()).isEqualTo(503);
                    assertThat(exception.getReason()).isEqualTo("AI model is currently unavailable. Please try again later.");
                });
    }

    @Test
    void chat_whenModelReturnsBlankContent_shouldFailInsteadOfReturningEmptySuccessfulAnswer() {
        stubChatClientRequestChain();
        AIAssistantServiceImpl service = serviceWithModel("qwen3", "ENABLED");
        when(callResponseSpec.content()).thenReturn("   ");

        assertThatThrownBy(() -> service.chat(new ChatRequestDTO("Analyze my spending"), jwt()))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode().value()).isEqualTo(502));
    }

    private static Stream<Arguments> thinkingModes() {
        return Stream.of(
                Arguments.of("LOW", "low"),
                Arguments.of(" medium ", "medium"),
                Arguments.of("HIGH", "high"),
                Arguments.of("ENABLED", true),
                Arguments.of("DISABLED", false),
                Arguments.of("unknown", false),
                Arguments.of(null, false));
    }

    private static Stream<Arguments> aiUnavailableExceptions() {
        return Stream.of(
                Arguments.of(new TransientAiException("temporary")),
                Arguments.of(new ResourceAccessException("ollama down")));
    }

    private AIAssistantServiceImpl serviceWithModel(String model, String thinkingMode) {
        return new AIAssistantServiceImpl(
                chatClient,
                new AssistantResponseSanitizer(),
                model,
                "30m",
                thinkingMode,
                8192,
                2048,
                0.2,
                7);
    }

    private void stubChatClientRequestChain() {
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.advisors(anyConsumer())).thenReturn(requestSpec);
        when(requestSpec.user(any(String.class))).thenReturn(requestSpec);
        when(requestSpec.options(any(OllamaChatOptions.class))).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
    }

    private static Jwt jwt() {
        return Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject(USER_ID.toString())
                .build();
    }

    @SuppressWarnings("unchecked")
    private static Consumer<ChatClient.AdvisorSpec> anyConsumer() {
        return any(Consumer.class);
    }
}
