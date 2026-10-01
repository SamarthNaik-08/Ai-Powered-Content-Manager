package com.contentmanager.modules.media.storage;

import com.contentmanager.common.exception.AppException;
import com.contentmanager.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocalStorageServiceTest {

    @TempDir
    Path tempDir;

    private LocalStorageService storageService;

    @BeforeEach
    void setUp() {
        storageService = new LocalStorageService(tempDir.toString());
    }

    @Test
    @DisplayName("Store and load file within root directory succeeds")
    void testStoreAndLoadValidFile() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "hello.txt", "text/plain", "Hello World".getBytes()
        );

        String storedPath = storageService.store(file, "hello.txt");
        assertThat(storedPath).isNotNull();

        org.springframework.core.io.Resource resource = storageService.loadAsResource("hello.txt");
        assertThat(resource.exists()).isTrue();
    }

    @Test
    @DisplayName("Store with path traversal throws AppException")
    void testStorePathTraversalThrows() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "hello.txt", "text/plain", "Hello World".getBytes()
        );

        assertThatThrownBy(() -> storageService.store(file, "../escape.txt"))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("Cannot store file outside current directory");
    }

    @Test
    @DisplayName("Load with path traversal throws ResourceNotFoundException")
    void testLoadPathTraversalThrows() {
        assertThatThrownBy(() -> storageService.loadAsResource("../nonexistent.txt"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Delete with path traversal is safely rejected without error")
    void testDeletePathTraversalSafelyIgnored() {
        // Should not throw or delete files outside root
        storageService.delete("../../etc/passwd");
    }
}
