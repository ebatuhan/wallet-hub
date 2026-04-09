package com.batu.plaid_adapter_service.messaging;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import com.batu.plaid_adapter_service.entity.Connection;
import com.batu.plaid_adapter_service.service.ConnectionService;
import com.batu.plaid_adapter_service.service.FetchService;
import com.batu.plaid_adapter_service.service.LinkService;
import com.batu.shared.messaging.saga.FetchAccountsCommand;
import com.batu.shared.messaging.saga.FetchAccountsEvent;
import com.batu.shared.messaging.saga.SagaChannels;

public class FetchAccountsListener {

    private final RabbitTemplate rabbitTemplate;
    private final ConnectionService connectionService;
    private final FetchService fetchService;

    public FetchAccountsListener(RabbitTemplate rabbitTemplate, ConnectionService connectionService,
            FetchService fetchService) {
        this.rabbitTemplate = rabbitTemplate;
        this.connectionService = connectionService;
        this.fetchService = fetchService;
    }

    @RabbitListener(queues=SagaChannels.FETCH_ACCOUNTS_COMMAND_QUEUE)
    private void fetchAccountsHandler(FetchAccountsCommand command){

        Connection connection = connectionService.readById(command.getConnectionId());

        try{
        fetchService.fetchAccounts(connection);

        rabbitTemplate.convertAndSend(
            SagaChannels.EXCHANGE,
            SagaChannels.FETCH_ACCOUNTS_EVENT_KEY,
            new FetchAccountsEvent(command.getSagaId(), null, true)); //ignore null
        }

        catch(Exception ex){
                    rabbitTemplate.convertAndSend(
            SagaChannels.EXCHANGE,
            SagaChannels.FETCH_ACCOUNTS_EVENT_KEY,
            new FetchAccountsEvent(command.getSagaId(), null, false)); //ignore null
        }
            throw ex;
        }

    }

}
