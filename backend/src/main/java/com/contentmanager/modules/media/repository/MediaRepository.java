package com.contentmanager.modules.media.repository;

import com.contentmanager.modules.media.model.Media;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MediaRepository extends JpaRepository<Media, Long> {

    Optional<Media> findByUuid(UUID uuid);

    Optional<Media> findByFilename(String filename);

    Page<Media> findByUploadedByIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    List<Media> findByContentId(Long contentId);

    @Query("SELECT m FROM Media m WHERE " +
           "(:keyword IS NULL OR LOWER(m.originalFilename) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(m.mimeType) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Media> searchMedia(@Param("keyword") String keyword, Pageable pageable);
}
