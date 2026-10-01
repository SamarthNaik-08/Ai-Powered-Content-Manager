package com.contentmanager.modules.content.service;

import com.contentmanager.common.exception.DuplicateResourceException;
import com.contentmanager.common.exception.ResourceNotFoundException;
import com.contentmanager.modules.content.dto.*;
import com.contentmanager.modules.content.model.Tag;
import com.contentmanager.modules.content.repository.TagRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TagService Tests")
class TagServiceTest {

    @Mock private TagRepository tagRepository;

    @InjectMocks private TagService tagService;

    private Tag testTag;

    @BeforeEach
    void setUp() {
        testTag = Tag.builder()
                .id(1L)
                .name("Featured")
                .slug("featured")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("getAllTags returns all tags")
    void getAllTags_success() {
        when(tagRepository.findAll()).thenReturn(List.of(testTag));

        List<TagResponse> result = tagService.getAllTags();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Featured");
    }

    @Test
    @DisplayName("createTag creates a tag with auto slug")
    void createTag_success() {
        CreateTagRequest request = CreateTagRequest.builder()
                .name("Tutorial")
                .build();

        when(tagRepository.existsByName("Tutorial")).thenReturn(false);
        when(tagRepository.existsBySlug("tutorial")).thenReturn(false);
        when(tagRepository.save(any(Tag.class))).thenAnswer(inv -> {
            Tag t = inv.getArgument(0);
            t.setId(2L);
            t.setCreatedAt(Instant.now());
            t.setUpdatedAt(Instant.now());
            return t;
        });

        TagResponse response = tagService.createTag(request);

        assertThat(response.getName()).isEqualTo("Tutorial");
        assertThat(response.getSlug()).isEqualTo("tutorial");
    }

    @Test
    @DisplayName("createTag throws on duplicate name")
    void createTag_duplicate() {
        CreateTagRequest request = CreateTagRequest.builder()
                .name("Featured")
                .build();

        when(tagRepository.existsByName("Featured")).thenReturn(true);

        assertThatThrownBy(() -> tagService.createTag(request))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    @DisplayName("getTagById returns tag")
    void getTagById_success() {
        when(tagRepository.findById(1L)).thenReturn(Optional.of(testTag));

        TagResponse response = tagService.getTagById(1L);

        assertThat(response.getName()).isEqualTo("Featured");
    }

    @Test
    @DisplayName("getTagById throws for non-existent id")
    void getTagById_notFound() {
        when(tagRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tagService.getTagById(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("deleteTag removes tag")
    void deleteTag_success() {
        when(tagRepository.existsById(1L)).thenReturn(true);

        tagService.deleteTag(1L);

        verify(tagRepository).deleteById(1L);
    }

    @Test
    @DisplayName("deleteTag throws for non-existent id")
    void deleteTag_notFound() {
        when(tagRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> tagService.deleteTag(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
