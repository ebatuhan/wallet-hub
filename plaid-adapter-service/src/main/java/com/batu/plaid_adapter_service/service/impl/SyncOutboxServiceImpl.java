package com.batu.plaid_adapter_service.service.impl;

import org.springframework.stereotype.Service;

import com.batu.plaid_adapter_service.entity.SyncOutboxMessage;
import com.batu.plaid_adapter_service.entity.enums.SyncOutboxMessageType;
import com.batu.plaid_adapter_service.exception.ProcessingException;
import com.batu.plaid_adapter_service.repository.SyncOutboxMessageRepository;
import com.batu.plaid_adapter_service.service.SyncOutboxService;
import com.batu.shared.messaging.MessagingTopology;
import com.batu.shared.messaging.command.AccountSyncCommand;
import com.batu.shared.messaging.command.TransactionSyncCommand;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Service
public class SyncOutboxServiceImpl implements SyncOutboxService {

    private final SyncOutboxMessageRepository syncOutboxMessageRepository;
    private final ObjectMapper objectMapper;

    public SyncOutboxServiceImpl(SyncOutboxMessageRepository syncOutboxMessageRepository, ObjectMapper objectMapper) {
        this.syncOutboxMessageRepository = syncOutboxMessageRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public void enqueueAccountCreate(AccountSyncCommand command) {
        enqueue(SyncOutboxMessageType.ACCOUNT_CREATE, MessagingTopology.ACCOUNT_CREATE_ROUTING_KEY, command);
    }

    @Override
    public void enqueueAccountUpdate(AccountSyncCommand command) {
        enqueue(SyncOutboxMessageType.ACCOUNT_UPDATE, MessagingTopology.ACCOUNT_UPDATE_ROUTING_KEY, command);
    }

    @Override
    public void enqueueTransactionCreate(TransactionSyncCommand command) {
        enqueue(SyncOutboxMessageType.TRANSACTION_CREATE, MessagingTopology.TRANSACTION_CREATE_ROUTING_KEY, command);
    }

    @Override
    public void enqueueTransactionUpdate(TransactionSyncCommand command) {
        enqueue(SyncOutboxMessageType.TRANSACTION_UPDATE, MessagingTopology.TRANSACTION_UPDATE_ROUTING_KEY, command);
    }

    private void enqueue(SyncOutboxMessageType messageType, String routingKey, Object payload) {
        syncOutboxMessageRepository.save(new SyncOutboxMessage(messageType, routingKey, serialize(payload)));
    }

    private String serialize(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JacksonException ex) {
            throw new ProcessingException("OUTBOX_SERIALIZATION_ERROR", "Failed to serialize sync outbox payload", ex);
        }
    }
}
