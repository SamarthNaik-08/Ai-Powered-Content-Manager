package com.contentmanager.modules.media.storage;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface StorageService {

    String store(MultipartFile file, String uniqueFilename);

    Resource loadAsResource(String filename);

    void delete(String filename);
}
