package com.batu.saga_orchestration.service.impl;

import java.util.UUID;

import com.batu.saga_orchestration.entity.LinkSaga;
import com.batu.saga_orchestration.repository.SagaRepository;
import com.batu.saga_orchestration.service.SagaService;

import jakarta.transaction.Transactional;

public class SagaServiceImpl implements SagaService {
    private final SagaRepository sagaRepository;

    public SagaServiceImpl(SagaRepository sagaRepository) {
        this.sagaRepository = sagaRepository;
    }

    @Override
    @Transactional
    public LinkSaga saveSaga(LinkSaga linkSaga) {
        return sagaRepository.save(linkSaga);
    }

    @Override
    @Transactional
    public LinkSaga updateSaga(UUID linkSagaId, LinkSaga linkSaga) {
        LinkSaga sagaToUpdate = sagaRepository.findById(linkSagaId)
                .orElseThrow(() -> new RuntimeException("Saga not found"));

        sagaToUpdate.setStatus(linkSaga.getStatus());
        sagaToUpdate.setUserId(linkSaga.getUserId());

        return sagaRepository.save(sagaToUpdate);
    }

    @Override
    public LinkSaga readById(UUID linkSagaId) {
        return sagaRepository.findById(linkSagaId)
                .orElseThrow(() -> new RuntimeException("Saga not found."));
    }

}
