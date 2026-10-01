package com.contentmanager.modules.ai.repository;

import com.contentmanager.modules.ai.model.AIRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AIRequestRepository extends JpaRepository<AIRequest, Long> {

    Optional<AIRequest> findByUuid(UUID uuid);

    Page<AIRequest> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Page<AIRequest> findByContentIdOrderByCreatedAtDesc(Long contentId, Pageable pageable);

    Page<AIRequest> findByContentIdAndUserIdOrderByCreatedAtDesc(Long contentId, Long userId, Pageable pageable);

    @Query("SELECT COUNT(r) FROM AIRequest r WHERE r.user.id = :userId AND r.createdAt >= :after")
    long countByUserIdAndCreatedAtAfter(@Param("userId") Long userId, @Param("after") Instant after);

    @Query("SELECT r.operation, COUNT(r) FROM AIRequest r GROUP BY r.operation")
    java.util.List<Object[]> countGroupedByOperation();

    @Query("SELECT r.provider, COUNT(r) FROM AIRequest r GROUP BY r.provider")
    java.util.List<Object[]> countGroupedByProvider();

    @Query("SELECT r.model, COUNT(r) FROM AIRequest r GROUP BY r.model")
    java.util.List<Object[]> countGroupedByModel();
}
