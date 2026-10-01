package com.contentmanager.modules.content.repository;

import com.contentmanager.modules.content.model.Content;
import com.contentmanager.modules.content.model.ContentStatus;
import com.contentmanager.modules.content.model.ContentType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ContentRepository extends JpaRepository<Content, Long>, JpaSpecificationExecutor<Content> {

    Optional<Content> findByUuid(UUID uuid);

    Optional<Content> findBySlug(String slug);

    boolean existsBySlug(String slug);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"author", "category"})
    Page<Content> findAll(org.springframework.data.jpa.domain.Specification<Content> spec, Pageable pageable);

    Page<Content> findByStatus(ContentStatus status, Pageable pageable);

    Page<Content> findByAuthorId(Long authorId, Pageable pageable);

    Page<Content> findByCategoryId(Long categoryId, Pageable pageable);

    Page<Content> findByContentType(ContentType contentType, Pageable pageable);

    @Query("SELECT COUNT(c) FROM Content c WHERE c.status = :status")
    long countByStatus(@Param("status") ContentStatus status);

    @Query("SELECT c FROM Content c LEFT JOIN FETCH c.author LEFT JOIN FETCH c.category LEFT JOIN FETCH c.tags WHERE c.id = :id")
    Optional<Content> findByIdWithRelations(@Param("id") Long id);

    java.util.List<Content> findByStatusAndScheduledAtLessThanEqual(ContentStatus status, java.time.Instant scheduledAt);

    @org.springframework.data.jpa.repository.Modifying
    @Query("UPDATE Content c SET c.status = :newStatus, c.publishedAt = :publishedAt, c.updatedAt = :now WHERE c.id = :id AND c.status = :expectedStatus")
    int updateStatusAtomically(
            @Param("id") Long id,
            @Param("expectedStatus") ContentStatus expectedStatus,
            @Param("newStatus") ContentStatus newStatus,
            @Param("publishedAt") java.time.Instant publishedAt,
            @Param("now") java.time.Instant now
    );

    @Query("SELECT COALESCE(c.category.name, 'Uncategorized'), COUNT(c) FROM Content c GROUP BY c.category.name")
    java.util.List<Object[]> countGroupedByCategory();

    @Query("SELECT c.contentType, COUNT(c) FROM Content c GROUP BY c.contentType")
    java.util.List<Object[]> countGroupedByContentType();

    @Query("SELECT COALESCE(c.author.name, 'Unknown'), COUNT(c) FROM Content c GROUP BY c.author.name")
    java.util.List<Object[]> countGroupedByAuthor();
}

