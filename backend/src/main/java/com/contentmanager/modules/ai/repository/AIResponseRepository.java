package com.contentmanager.modules.ai.repository;

import com.contentmanager.modules.ai.model.AIResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AIResponseRepository extends JpaRepository<AIResponse, Long> {

    Optional<AIResponse> findByUuid(UUID uuid);

    Optional<AIResponse> findByRequestId(Long requestId);

    @Query("SELECT r.status, COUNT(r) FROM AIResponse r GROUP BY r.status")
    java.util.List<Object[]> countGroupedByStatus();

    @Query("SELECT COALESCE(SUM(r.totalTokens), 0) FROM AIResponse r")
    long sumTotalTokens();

    @Query("SELECT COALESCE(AVG(r.executionTimeMs), 0.0) FROM AIResponse r")
    double avgExecutionTimeMs();
}
