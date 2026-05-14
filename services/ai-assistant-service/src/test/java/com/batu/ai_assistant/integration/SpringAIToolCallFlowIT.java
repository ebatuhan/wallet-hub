package com.batu.ai_assistant.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.ToolCallAdvisor;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.messages.AbstractMessage;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.http.ResponseEntity;

import com.batu.ai_assistant.advisor.PromptGuardAdvisor;
import com.batu.ai_assistant.client.DashboardClient;
import com.batu.ai_assistant.dto.client.UserDashboardSummaryResponseDto;
import com.batu.ai_assistant.tools.DashboardTools;

class SpringAIToolCallFlowIT {

    private static final String CONVERSATION_ID = "spring-ai-tool-flow";
    private static final UUID USER_ID = UUID.fromString("c4000000-0000-0000-0000-000000000001");

    @Test
    void chatClient_whenModelRequestsTool_shouldExecuteToolFeedResultBackAndPersistMemoryAfterPromptGuardAllows() {
        FakeToolCallingChatModel chatModel = new FakeToolCallingChatModel();
        DashboardClient dashboardClient = org.mockito.Mockito.mock(DashboardClient.class);
        UserDashboardSummaryResponseDto dashboardSummary = dashboardSummary();
        when(dashboardClient.getSummary(null)).thenReturn(ResponseEntity.ok(dashboardSummary));
        ChatMemory chatMemory = MessageWindowChatMemory.builder().build();
        ChatClient guardChatClient = ChatClient.builder(chatModel).build();
        ChatClient assistantChatClient = ChatClient.builder(chatModel)
                .defaultTools(new DashboardTools(dashboardClient))
                .defaultAdvisors(
                        new PromptGuardAdvisor(guardChatClient),
                        MessageChatMemoryAdvisor.builder(chatMemory)
                                .order(BaseAdvisor.HIGHEST_PRECEDENCE + 200)
                                .build(),
                        ToolCallAdvisor.builder()
                                .advisorOrder(BaseAdvisor.HIGHEST_PRECEDENCE + 300)
                                .build())
                .build();

        String content = assistantChatClient.prompt()
                .advisors(advisors -> advisors.param(ChatMemory.CONVERSATION_ID, CONVERSATION_ID))
                .user("Analyze my spending this month")
                .call()
                .content();

        assertThat(content).isEqualTo("I reviewed your dashboard summary and found one active account.");
        verify(dashboardClient).getSummary(null);
        assertThat(chatModel.guardPrompts).hasSize(1);
        assertThat(chatModel.guardPrompts.getFirst()).contains("Analyze my spending this month");
        assertThat(chatModel.assistantPrompts).hasSize(2);
        assertThat(chatModel.assistantPrompts.get(1).getInstructions())
                .anySatisfy(message -> assertThat(message).isInstanceOfSatisfying(
                        ToolResponseMessage.class,
                        toolResponseMessage -> assertThat(toolResponseMessage.getResponses())
                                .singleElement()
                                .satisfies(toolResponse -> {
                                    assertThat(toolResponse.name()).isEqualTo("get_dashboard_summary");
                                    assertThat(toolResponse.responseData()).contains("Dashboard summary loaded");
                                })));
        assertThat(chatMemory.get(CONVERSATION_ID))
                .extracting(SpringAIToolCallFlowIT::messageText)
                .anySatisfy(text -> assertThat(text).contains("Analyze my spending this month"))
                .anySatisfy(text -> assertThat(text).contains("I reviewed your dashboard summary"));
    }

    private static String messageText(Message message) {
        return message instanceof AbstractMessage abstractMessage ? abstractMessage.getText() : message.toString();
    }

    private static UserDashboardSummaryResponseDto dashboardSummary() {
        return new UserDashboardSummaryResponseDto(
                USER_ID,
                new UserDashboardSummaryResponseDto.PeriodDto(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31)),
                new UserDashboardSummaryResponseDto.AccountSummaryDto(1, List.of()),
                new UserDashboardSummaryResponseDto.IncomeSectionDto(List.of()),
                new UserDashboardSummaryResponseDto.RecentTransactionsDto(List.of(), false, null),
                new UserDashboardSummaryResponseDto.SpendingSectionDto(List.of(
                        new UserDashboardSummaryResponseDto.SpendingCurrencyGroupDto(
                                "USD",
                                new BigDecimal("42.00"),
                                List.of())), null));
    }

    private static final class FakeToolCallingChatModel implements ChatModel {

        private final List<String> guardPrompts = new ArrayList<>();
        private final List<Prompt> assistantPrompts = new ArrayList<>();
        private int assistantCalls;

        @Override
        public ChatResponse call(Prompt prompt) {
            if (prompt.getContents().contains("USER_INPUT_START")) {
                guardPrompts.add(prompt.getContents());
                return response("{\"decision\":\"ALLOW\",\"reason\":\"safe\"}");
            }

            assistantPrompts.add(prompt);
            assistantCalls++;
            if (assistantCalls == 1) {
                AssistantMessage.ToolCall toolCall = new AssistantMessage.ToolCall(
                        "call-dashboard-summary",
                        "function",
                        "get_dashboard_summary",
                        "{}");
                return new ChatResponse(List.of(new Generation(AssistantMessage.builder()
                        .content("")
                        .toolCalls(List.of(toolCall))
                        .build())));
            }

            assertThat(prompt.getInstructions()).anyMatch(ToolResponseMessage.class::isInstance);
            return response("I reviewed your dashboard summary and found one active account.");
        }

        @Override
        public ChatOptions getDefaultOptions() {
            return null;
        }

        private static ChatResponse response(String content) {
            return new ChatResponse(List.of(new Generation(new AssistantMessage(content))));
        }
    }
}
