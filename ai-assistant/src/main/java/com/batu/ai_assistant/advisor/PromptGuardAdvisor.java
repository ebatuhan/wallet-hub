package com.batu.ai_assistant.advisor;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

import com.batu.ai_assistant.dto.PromptSafetyDecisionDTO;
import com.batu.ai_assistant.dto.PromptSafetyDecisionType;
import com.batu.ai_assistant.exception.PromptBlockedException;

@Component
public class PromptGuardAdvisor implements CallAdvisor {

    private final ChatClient guardChatClient;

    public PromptGuardAdvisor(ChatClient guardChatClient) {
        this.guardChatClient = guardChatClient;
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        String userText = request.prompt().getUserMessage() == null ? "" : request.prompt().getUserMessage().getText();

        try {
            PromptSafetyDecisionDTO decision = guardChatClient.prompt()
                    .user(wrapGuardPrompt(userText))
                    .call()
                    .entity(PromptSafetyDecisionDTO.class);

            if (decision == null || decision.decision() != PromptSafetyDecisionType.ALLOW) {
                throw new PromptBlockedException(reason(decision));
            }
        } catch (PromptBlockedException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new PromptBlockedException("Request blocked because prompt safety guard failed.");
        }

        return chain.nextCall(request);
    }

    @Override
    public String getName() {
        return "prompt-guard-advisor";
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    private String wrapGuardPrompt(String userMessage) {
        return """
                USER_INPUT_START
                %s
                USER_INPUT_END

                Decide whether the content inside USER_INPUT tags is safe to send to the financial assistant.
                Return only structured output with:
                - decision: ALLOW or BLOCK
                - reason: short explanation
                """.formatted(userMessage);
    }

    private String reason(PromptSafetyDecisionDTO decision) {
        if (decision == null || decision.reason() == null || decision.reason().isBlank()) {
            return "Request blocked by prompt safety guard.";
        }
        return decision.reason();
    }
}
