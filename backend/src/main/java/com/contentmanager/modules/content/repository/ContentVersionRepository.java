package com.contentmanager.modules.content.repository;

import com.contentmanager.modules.content.model.ContentVersion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ContentVersionRepository extends JpaRepository<ContentVersion, Long> {

    Page<ContentVersion> findByContentIdOrderByVersionNumberDesc(Long contentId, Pageable pageable);

    Optional<ContentVersion> findByContentIdAndVersionNumber(Long contentId, Integer versionNumber);

    @Query("SELECT COALESCE(MAX(cv.versionNumber), 0) FROM ContentVersion cv WHERE cv.content.id = :contentId")
    int findMaxVersionNumber(@Param("contentId") Long contentId);
}
