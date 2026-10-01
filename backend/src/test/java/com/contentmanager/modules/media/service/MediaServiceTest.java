package com.contentmanager.modules.media.service;

import com.contentmanager.common.exception.AppException;
import com.contentmanager.modules.audit.service.AuditLogService;
import com.contentmanager.modules.content.repository.ContentRepository;
import com.contentmanager.modules.media.dto.MediaResponse;
import com.contentmanager.modules.media.model.Media;
import com.contentmanager.modules.media.repository.MediaRepository;
import com.contentmanager.modules.media.storage.StorageService;
import com.contentmanager.modules.user.model.User;
import com.contentmanager.modules.user.repository.UserRepository;
import com.contentmanager.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MediaService Tests")
class MediaServiceTest {

    @Mock private MediaRepository mediaRepository;
    @Mock private ContentRepository contentRepository;
    @Mock private UserRepository userRepository;
    @Mock private StorageService storageService;
    @Mock private AuditLogService auditLogService;

    private MediaService mediaService;
    private User testUser;
    private UserPrincipal adminPrincipal;
    private UserPrincipal authorPrincipal;
    private UserPrincipal otherPrincipal;

    @BeforeEach
    void setUp() {
        mediaService = new MediaService(
                mediaRepository,
                contentRepository,
                userRepository,
                storageService,
                auditLogService,
                5 * 1024 * 1024L, // 5MB
                List.of("image/jpeg", "image/png", "image/webp")
        );

        testUser = User.builder().id(1L).email("author@example.com").name("Author").build();

        adminPrincipal = UserPrincipal.builder()
                .id(99L)
                .email("admin@example.com")
                .authorities(Set.of(new SimpleGrantedAuthority("ROLE_ADMIN")))
                .build();

        authorPrincipal = UserPrincipal.builder()
                .id(1L)
                .email("author@example.com")
                .authorities(Set.of(new SimpleGrantedAuthority("ROLE_AUTHOR")))
                .build();

        otherPrincipal = UserPrincipal.builder()
                .id(2L)
                .email("other@example.com")
                .authorities(Set.of(new SimpleGrantedAuthority("ROLE_AUTHOR")))
                .build();
    }

    @Test
    @DisplayName("Upload valid image successfully")
    void uploadMedia_success() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.png", "image/png", "fake-image-bytes".getBytes()
        );

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(storageService.store(any(), anyString())).thenReturn("photo-123.png");
        when(mediaRepository.save(any(Media.class))).thenAnswer(inv -> {
            Media m = inv.getArgument(0);
            m.setId(10L);
            return m;
        });

        MediaResponse response = mediaService.uploadMedia(file, null, authorPrincipal);

        assertThat(response).isNotNull();
        assertThat(response.getOriginalFilename()).isEqualTo("photo.png");
        verify(storageService).store(any(), anyString());
        verify(mediaRepository).save(any(Media.class));
    }

    @Test
    @DisplayName("Upload rejects disallowed MIME types")
    void uploadMedia_disallowedMimeType_throwsException() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "script.exe", "application/x-msdownload", "binary".getBytes()
        );

        assertThatThrownBy(() -> mediaService.uploadMedia(file, null, authorPrincipal))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("not supported");

        verifyNoInteractions(storageService);
    }

    @Test
    @DisplayName("Upload rejects file exceeding maximum size limit")
    void uploadMedia_oversizedFile_throwsException() {
        byte[] largeBytes = new byte[6 * 1024 * 1024]; // 6MB
        MockMultipartFile file = new MockMultipartFile(
                "file", "big.png", "image/png", largeBytes
        );

        assertThatThrownBy(() -> mediaService.uploadMedia(file, null, authorPrincipal))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("exceeds maximum allowed limit");

        verifyNoInteractions(storageService);
    }

    @Test
    @DisplayName("Upload rejects empty file")
    void uploadMedia_emptyFile_throwsException() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "empty.png", "image/png", new byte[0]
        );

        assertThatThrownBy(() -> mediaService.uploadMedia(file, null, authorPrincipal))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("empty or not provided");

        verifyNoInteractions(storageService);
    }

    @Test
    @DisplayName("Delete media as owner succeeds")
    void deleteMedia_asOwner_success() {
        Media media = Media.builder()
                .id(1L)
                .filename("unique-test.png")
                .uploadedBy(testUser)
                .build();

        when(mediaRepository.findById(1L)).thenReturn(Optional.of(media));

        mediaService.deleteMedia(1L, authorPrincipal);

        verify(storageService).delete("unique-test.png");
        verify(mediaRepository).delete(media);
    }

    @Test
    @DisplayName("Delete media as admin succeeds even if not owner")
    void deleteMedia_asAdmin_success() {
        Media media = Media.builder()
                .id(1L)
                .filename("unique-test.png")
                .uploadedBy(testUser)
                .build();

        when(mediaRepository.findById(1L)).thenReturn(Optional.of(media));

        mediaService.deleteMedia(1L, adminPrincipal);

        verify(storageService).delete("unique-test.png");
        verify(mediaRepository).delete(media);
    }

    @Test
    @DisplayName("Delete media by non-owner non-admin throws AccessDeniedException")
    void deleteMedia_byNonOwner_throwsAccessDenied() {
        Media media = Media.builder()
                .id(1L)
                .filename("unique-test.png")
                .uploadedBy(testUser)
                .build();

        when(mediaRepository.findById(1L)).thenReturn(Optional.of(media));

        assertThatThrownBy(() -> mediaService.deleteMedia(1L, otherPrincipal))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("You do not have permission to delete this media file");

        verify(storageService, never()).delete(anyString());
        verify(mediaRepository, never()).delete(any());
    }
}
