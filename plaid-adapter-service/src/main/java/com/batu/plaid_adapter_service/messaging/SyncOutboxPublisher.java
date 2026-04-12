package com.batu.plaid_adapter_service.messaging;

import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import com.batu.plaid_adapter_service.entity.SyncOutboxMessage;
import com.batu.plaid_adapter_service.entity.enums.SyncOutboxMessageStatus;
import com.batu.plaid_adapter_service.exception.ProcessingException;
import com.batu.plaid_adapter_service.exception.SyncStateException;
import com.batu.plaid_adapter_service.repository.SyncOutboxMessageRepository;
import com.batu.shared.messaging.command.AccountSyncCommand;
import com.batu.shared.messaging.command.TransactionSyncCommand;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Component
public class SyncOutboxPublisher {

    private static final int MAX_MESSAGES_PER_RUN = 50;

    private final SyncOutboxMessageRepository syncOutboxMessageRepository;
    private final SyncCommandPublisher syncCommandPublisher;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transactionTemplate;

    public SyncOutboxPublisher(SyncOutboxMessageRepository syncOutboxMessageRepository,
            SyncCommandPublisher syncCommandPublisher,
            ObjectMapper objectMapper,
            TransactionTemplate transactionTemplate) {
        this.syncOutboxMessageRepository = syncOutboxMessageRepository;
        this.syncCommandPublisher = syncCommandPublisher;
        this.objectMapper = objectMapper;
        this.transactionTemplate = transactionTemplate;
    }

    @Scheduled(fixedDelayString = "${plaid.sync.outbox.publish-delay:1000}")
    public void publishPendingMessages() {
        List<SyncOutboxMessage> pendingMessages = claimPendingMessages();

        for (SyncOutboxMessage message : pendingMessages) {
            try {
                publish(message);
                markPublished(message);
            } catch (RuntimeException ex) {
                markPending(message);
                break;
            }
        }
    }

    protected List<SyncOutboxMessage> claimPendingMessages() {
        return transactionTemplate.execute(status -> {
            List<SyncOutboxMessage> pendingMessages = syncOutboxMessageRepository.findByStatusOrdered(
                    SyncOutboxMessageStatus.PENDING,
                    PageRequest.of(0, MAX_MESSAGES_PER_RUN));

            for (SyncOutboxMessage pendingMessage : pendingMessages) {
                pendingMessage.markProcessing();
            }

            return pendingMessages;
        });
    }

    protected void markPublished(SyncOutboxMessage message) {
        transactionTemplate.executeWithoutResult(status -> {
            SyncOutboxMessage storedMessage = syncOutboxMessageRepository.findById(message.getOutboxId())
                    .orElseThrow(() -> new SyncStateException(
                            "Outbox message " + message.getOutboxId() + " not found while marking published"));
            storedMessage.markPublished(Instant.now());
        });
    }

    protected void markPending(SyncOutboxMessage message) {
        transactionTemplate.executeWithoutResult(status -> {
            SyncOutboxMessage storedMessage = syncOutboxMessageRepository.findById(message.getOutboxId())
                    .orElseThrow(() -> new SyncStateException(
                            "Outbox message " + message.getOutboxId() + " not found while marking pending"));
            storedMessage.markPending();
        });
    }

    private void publish(SyncOutboxMessage message) {
        switch (message.getMessageType()) {
            case ACCOUNT_CREATE -> syncCommandPublisher.publishAccountCreate(readAccountCommand(message.getPayload()));
            case ACCOUNT_UPDATE -> syncCommandPublisher.publishAccountUpdate(readAccountCommand(message.getPayload()));
            case TRANSACTION_CREATE -> syncCommandPublisher
                    .publishTransactionCreate(readTransactionCommand(message.getPayload()));
            case TRANSACTION_UPDATE -> syncCommandPublisher
                    .publishTransactionUpdate(readTransactionCommand(message.getPayload()));
        }
    }

    private AccountSyncCommand readAccountCommand(String payload) {
        try {
            return objectMapper.readValue(payload, AccountSyncCommand.class);
        } catch (JacksonException ex) {
            throw new ProcessingException("OUTBOX_DESERIALIZATION_ERROR",
                    "Failed to deserialize account sync command", ex);
        }
    }

    private TransactionSyncCommand readTransactionCommand(String payload) {
        try {
            return objectMapper.readValue(payload, TransactionSyncCommand.class);
        } catch (JacksonException ex) {
            throw new ProcessingException("OUTBOX_DESERIALIZATION_ERROR",
                    "Failed to deserialize transaction sync command", ex);
        }
    }
}
