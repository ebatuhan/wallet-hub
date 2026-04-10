package com.batu.account_service.messaging;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import com.batu.account_service.service.AccountService;
import com.batu.shared.dto.request.AccountsUpsertRequestDto;
import com.batu.shared.messaging.saga.InitialAccountsPersistedEvent;
import com.batu.shared.messaging.saga.PersistInitialAccountsCommand;
import com.batu.shared.messaging.saga.SagaChannels;

@Component
public class InitialAccountsPersistenceListener {

    private final AccountService accountService;
    private final RabbitTemplate rabbitTemplate;

    public InitialAccountsPersistenceListener(AccountService accountService, RabbitTemplate rabbitTemplate) {
        this.accountService = accountService;
        this.rabbitTemplate = rabbitTemplate;
    }

    @RabbitListener(queues = SagaChannels.PERSIST_INITIAL_ACCOUNTS_COMMAND_QUEUE)
    public void persistInitialAccounts(PersistInitialAccountsCommand command) {
        var response = accountService.upsertAccounts(new AccountsUpsertRequestDto(command.getAccounts()));

        rabbitTemplate.convertAndSend(
                SagaChannels.EXCHANGE,
                SagaChannels.INITIAL_ACCOUNTS_PERSISTED_EVENT_KEY,
                new InitialAccountsPersistedEvent(
                        command.getSagaId(),
                        command.getConnectionId(),
                        response.getInsertedAccountsMap()));
    }
}
