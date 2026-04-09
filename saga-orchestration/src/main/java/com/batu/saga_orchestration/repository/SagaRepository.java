package com.batu.saga_orchestration.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.batu.saga_orchestration.entity.LinkSaga;

@Repository
public interface SagaRepository extends JpaRepository<LinkSaga, UUID> {

}
