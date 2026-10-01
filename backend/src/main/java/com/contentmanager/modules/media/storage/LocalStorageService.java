package com.contentmanager.modules.media.storage;

import com.contentmanager.common.exception.AppException;
import com.contentmanager.common.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.*;

@Slf4j
@Service
public class LocalStorageService implements StorageService {

    private final Path rootLocation;

    public LocalStorageService(@Value("${app.media.upload-dir:./uploads}") String uploadDir) {
        this.rootLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.rootLocation);
        } catch (IOException e) {
            log.error("Could not initialize storage location: {}", uploadDir, e);
            throw new AppException("Could not initialize storage directory", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public String store(MultipartFile file, String uniqueFilename) {
        try {
            if (file.isEmpty()) {
                throw new AppException("Failed to store empty file.", HttpStatus.BAD_REQUEST);
            }

            Path destinationFile = this.rootLocation.resolve(Paths.get(uniqueFilename))
                    .normalize().toAbsolutePath();

            if (!destinationFile.getParent().equals(this.rootLocation.toAbsolutePath())) {
                // Security: Prevent Directory Traversal Attack
                throw new AppException("Cannot store file outside current directory.", HttpStatus.BAD_REQUEST);
            }

            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, destinationFile, StandardCopyOption.REPLACE_EXISTING);
            }

            return destinationFile.toString();
        } catch (IOException e) {
            log.error("Failed to store file {}", uniqueFilename, e);
            throw new AppException("Failed to store file", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public Resource loadAsResource(String filename) {
        try {
            Path file = rootLocation.resolve(filename).normalize().toAbsolutePath();
            if (!file.startsWith(rootLocation.toAbsolutePath())) {
                log.warn("Path traversal attempt detected with filename: {}", filename);
                throw new ResourceNotFoundException("Media", "filename", filename);
            }
            Resource resource = new UrlResource(file.toUri());
            if (resource.exists() || resource.isReadable()) {
                return resource;
            } else {
                throw new ResourceNotFoundException("Media", "filename", filename);
            }
        } catch (MalformedURLException e) {
            throw new ResourceNotFoundException("Media", "filename", filename);
        }
    }

    @Override
    public void delete(String filename) {
        try {
            Path file = rootLocation.resolve(filename).normalize().toAbsolutePath();
            if (!file.startsWith(rootLocation.toAbsolutePath())) {
                log.warn("Path traversal attempt detected on delete with filename: {}", filename);
                return;
            }
            Files.deleteIfExists(file);
        } catch (IOException e) {
            log.warn("Failed to delete file on disk: {}", filename, e);
        }
    }
}
