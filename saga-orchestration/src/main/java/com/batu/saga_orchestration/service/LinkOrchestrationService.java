package com.batu.saga_orchestration.service;

import com.batu.shared.messaging.saga.FetchAccountsEvent;
import com.batu.shared.messaging.saga.FetchTransactionsEvent;
import com.batu.shared.messaging.saga.PersistAccountsEvent;
import com.batu.shared.messaging.saga.StartLinkSagaCommand;



public interface LinkOrchestrationService {
    void startLinkSaga(StartLinkSagaCommand startLinkSagaCommand);
    void onFetchAccountsEvent(FetchAccountsEvent fetchAccountsEvent);
    void onPersistAccountsEvent(PersistAccountsEvent persistAccountsEvent);
    void onFetchTransactionsEvent(FetchTransactionsEvent fetchTransactionsEvent);

}
