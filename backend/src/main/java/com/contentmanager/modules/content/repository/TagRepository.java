package com.contentmanager.modules.content.repository;

import com.contentmanager.modules.content.model.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.Set;

@Repository
public interface TagRepository extends JpaRepository<Tag, Long> {

    Optional<Tag> findBySlug(String slug);

    Optional<Tag> findByName(String name);

    boolean existsByName(String name);

    boolean existsBySlug(String slug);

    Set<Tag> findByIdIn(Set<Long> ids);
}
