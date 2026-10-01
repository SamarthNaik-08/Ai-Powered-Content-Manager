package com.contentmanager.modules.content.service;

import com.contentmanager.common.exception.DuplicateResourceException;
import com.contentmanager.common.exception.ResourceNotFoundException;
import com.contentmanager.modules.content.dto.*;
import com.contentmanager.modules.content.model.Tag;
import com.contentmanager.modules.content.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TagService {

    private final TagRepository tagRepository;

    @Transactional(readOnly = true)
    public List<TagResponse> getAllTags() {
        return tagRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public TagResponse getTagById(Long id) {
        Tag tag = tagRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tag not found with id: " + id));
        return mapToResponse(tag);
    }

    @Transactional
    public TagResponse createTag(CreateTagRequest request) {
        String name = request.getName().trim();
        if (tagRepository.existsByName(name)) {
            throw new DuplicateResourceException("Tag with name '" + name + "' already exists.");
        }

        String slug = generateSlug(name);
        Tag tag = Tag.builder()
                .name(name)
                .slug(slug)
                .build();

        Tag saved = tagRepository.save(tag);
        log.info("Tag created: '{}'", saved.getName());
        return mapToResponse(saved);
    }

    @Transactional
    public void deleteTag(Long id) {
        if (!tagRepository.existsById(id)) {
            throw new ResourceNotFoundException("Tag not found with id: " + id);
        }
        tagRepository.deleteById(id);
        log.info("Tag deleted: id={}", id);
    }

    private String generateSlug(String name) {
        String base = name.trim().toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");

        String slug = base;
        int counter = 1;
        while (tagRepository.existsBySlug(slug)) {
            slug = base + "-" + counter++;
        }
        return slug;
    }

    private TagResponse mapToResponse(Tag tag) {
        return TagResponse.builder()
                .id(tag.getId())
                .name(tag.getName())
                .slug(tag.getSlug())
                .createdAt(tag.getCreatedAt())
                .updatedAt(tag.getUpdatedAt())
                .build();
    }
}
