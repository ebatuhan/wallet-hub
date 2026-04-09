package com.batu.saga_orchestration.service;

import java.util.UUID;

import com.batu.saga_orchestration.entity.LinkSaga;

public interface SagaService {
    LinkSaga saveSaga(LinkSaga linkSaga);
    LinkSaga updateSaga(UUID linkSagaId, LinkSaga linkSaga);
    LinkSaga readById(UUID linkSagaId);

}
