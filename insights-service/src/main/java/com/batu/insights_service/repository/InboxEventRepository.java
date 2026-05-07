package com.batu.insights_service.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.batu.insights_service.entity.InboxEvent;

public interface InboxEventRepository extends JpaRepository<InboxEvent, UUID> {

    @Query(value = "select pg_advisory_xact_lock(hashtext(cast(:eventId as text)))", nativeQuery = true)
    void lockByEventId(@Param("eventId") UUID eventId);
}
