package com.batu.saga_orchestration.service.impl;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import com.batu.saga_orchestration.entity.LinkSaga;
import com.batu.saga_orchestration.entity.LinkSagaStatus;
import com.batu.saga_orchestration.service.LinkOrchestrationService;
import com.batu.saga_orchestration.service.SagaService;
import com.batu.shared.messaging.saga.FetchAccountsCommand;
import com.batu.shared.messaging.saga.FetchAccountsEvent;
import com.batu.shared.messaging.saga.FetchTransactionsCommand;
import com.batu.shared.messaging.saga.FetchTransactionsEvent;
import com.batu.shared.messaging.saga.PersistAccountsCommand;
import com.batu.shared.messaging.saga.PersistAccountsEvent;
import com.batu.shared.messaging.saga.SagaChannels;
import com.batu.shared.messaging.saga.StartLinkSagaCommand;

@Service
public class LinkOrchestrationServiceImpl implements LinkOrchestrationService {

    private final SagaService sagaService;
    private final RabbitTemplate rabbitTemplate;

    public LinkOrchestrationServiceImpl(SagaService sagaService, RabbitTemplate rabbitTemplate) {
        this.sagaService = sagaService;
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    @RabbitListener(queues = SagaChannels.START_LINK_SAGA_QUEUE)
    public void startLinkSaga(StartLinkSagaCommand startLinkSagaCommand) {

        System.out.println("Saga Started");

        LinkSaga linkSaga = LinkSaga.builder()
                .status(LinkSagaStatus.STARTED)
                .userId(startLinkSagaCommand.getUserId())
                .connectionId(startLinkSagaCommand.getConnectionId())
                .build();

        var savedSaga = sagaService.saveSaga(linkSaga);

        rabbitTemplate.convertAndSend(SagaChannels.EXCHANGE,
                SagaChannels.FETCH_ACCOUNTS_COMMAND_KEY,
                new FetchAccountsCommand(savedSaga.getLinkSagaId(), savedSaga.getConnectionId()));
    }

    @Override
    @RabbitListener(queues = SagaChannels.FETCH_ACCOUNTS_EVENT_QUEUE)
    public void onFetchAccountsEvent(FetchAccountsEvent fetchAccountsEvent) {

        System.out.println("Phase 2. Accounts are fetching");
        LinkSaga saga = sagaService.readById(fetchAccountsEvent.getSagaId());

        if (!fetchAccountsEvent.isSuccesfull()) {
            saga.setStatus(LinkSagaStatus.ACCOUNT_FAILED);
            sagaService.updateSaga(saga.getLinkSagaId(), saga);
            return;
        }

        rabbitTemplate.convertAndSend(SagaChannels.EXCHANGE,
                SagaChannels.PERSIST_ACCOUNTS_COMMAND_KEY,
                new PersistAccountsCommand(saga.getLinkSagaId(), fetchAccountsEvent.getAccountsPayload()));

        saga.setStatus(LinkSagaStatus.ACCOUNT_PENDING); // TODO change to ACCOUNT PERSIST PENDING

        sagaService.updateSaga(saga.getUserId(), saga);

    }

    @Override
    @RabbitListener(queues = SagaChannels.PERSIST_ACCOUNTS_EVENT_QUEUE)
    public void onPersistAccountsEvent(PersistAccountsEvent persistAccountsEvent) {

        System.out.println("Phase 3. Accounts are persisting.");

        LinkSaga saga = sagaService.readById(persistAccountsEvent.getSagaId());

        if (!persistAccountsEvent.isSuccesfull()) {
            saga.setStatus(LinkSagaStatus.ACCOUNT_FAILED); // TODO change to persist failed
            sagaService.updateSaga(saga.getLinkSagaId(), saga);
            return;
        }

        saga.setAccountIdMap(persistAccountsEvent.getAccountIdMap());

        rabbitTemplate.convertAndSend(SagaChannels.EXCHANGE,
                SagaChannels.FETCH_TRANSACTIONS_COMMAND_KEY,
                new FetchTransactionsCommand(saga.getLinkSagaId()));

        saga.setStatus(LinkSagaStatus.TRANSACTION_PENDING); // TODO change to ACCOUNT PERSIST PENDING

        sagaService.updateSaga(saga.getUserId(), saga);

    }

    @Override
    @RabbitListener(queues = SagaChannels.FETCH_TRANSACTIONS_EVENT_QUEUE)
    public void onFetchTransactionsEvent(FetchTransactionsEvent fetchTransactionsEvent) {

        System.out.println("Transactions are fetching.");
        LinkSaga saga = sagaService.readById(fetchTransactionsEvent.getSagaId());

        saga.setStatus(LinkSagaStatus.COMPLETED);

        sagaService.updateSaga(saga.getLinkSagaId(), saga);

        System.out.println("Saga completed.");
    }
}