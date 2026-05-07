package com.batu.ai_assistant.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.ai.tool.execution.ToolExecutionExceptionProcessor;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import com.batu.ai_assistant.advisor.PromptGuardAdvisor;
import com.batu.ai_assistant.config.SpringAIConfiguration;
import com.batu.ai_assistant.tools.BudgetTools;
import com.batu.ai_assistant.tools.DashboardTools;
import com.batu.ai_assistant.tools.LookupTools;

@SpringJUnitConfig(classes = { SpringAIConfiguration.class, SpringAIConfigurationIT.TestBeans.class })
@TestPropertySource(properties = {
        "spring.cloud.config.enabled=false",
        "spring.config.import=",
        "spring.ai.ollama.chat.options.model=qwen3",
        "assistant.guard.model=qwen3"
})
class SpringAIConfigurationIT {

    @org.springframework.beans.factory.annotation.Autowired
    private ChatMemory chatMemory;

    @org.springframework.beans.factory.annotation.Autowired
    private ToolExecutionExceptionProcessor toolExecutionExceptionProcessor;

    @org.springframework.beans.factory.annotation.Autowired
    private ChatClient.Builder chatClientBuilder;

    @Test
    void context_whenSpringAiConfigurationLoads_shouldWireMemoryClientsToolsAndExceptionProcessor() {
        assertThat(chatMemory).isNotNull();
        assertThat(toolExecutionExceptionProcessor).isNotNull();
        verify(chatClientBuilder).defaultTools(any(DashboardTools.class), any(BudgetTools.class), any(LookupTools.class));
        verify(chatClientBuilder).defaultAdvisors(any(Advisor.class), any(Advisor.class), any(Advisor.class));
        verify(chatClientBuilder).defaultOptions(any(OllamaChatOptions.class));
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class TestBeans {

        @Bean
        ChatClient.Builder chatClientBuilder() {
            ChatClient.Builder builder = mock(ChatClient.Builder.class);
            when(builder.defaultSystem(anyString())).thenReturn(builder);
            when(builder.defaultTools(any(Object.class), any(Object.class), any(Object.class))).thenReturn(builder);
            when(builder.defaultAdvisors(any(Advisor.class), any(Advisor.class), any(Advisor.class))).thenReturn(builder);
            when(builder.defaultOptions(any(OllamaChatOptions.class))).thenReturn(builder);
            when(builder.build()).thenReturn(mock(ChatClient.class));
            return builder;
        }

        @Bean
        ChatMemoryRepository chatMemoryRepository() {
            return mock(ChatMemoryRepository.class);
        }

        @Bean
        PromptGuardAdvisor promptGuardAdvisor() {
            return mock(PromptGuardAdvisor.class);
        }

        @Bean
        DashboardTools dashboardTools() {
            return mock(DashboardTools.class);
        }

        @Bean
        BudgetTools budgetTools() {
            return mock(BudgetTools.class);
        }

        @Bean
        LookupTools lookupTools() {
            return mock(LookupTools.class);
        }
    }
}
