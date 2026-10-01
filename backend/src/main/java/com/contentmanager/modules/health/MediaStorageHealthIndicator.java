package com.contentmanager.modules.health;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.io.File;

@Component
public class MediaStorageHealthIndicator implements HealthIndicator {

    @Value("${app.media.upload-dir:./uploads}")
    private String uploadDir;

    @Override
    public Health health() {
        File dir = new File(uploadDir);
        if (!dir.exists()) {
            boolean created = dir.mkdirs();
            if (!created && !dir.exists()) {
                return Health.down()
                        .withDetail("storagePath", dir.getAbsolutePath())
                        .withDetail("error", "Directory does not exist and could not be created")
                        .build();
            }
        }

        if (!dir.canWrite()) {
            return Health.down()
                    .withDetail("storagePath", dir.getAbsolutePath())
                    .withDetail("error", "Directory is not writable")
                    .build();
        }

        long freeSpaceBytes = dir.getFreeSpace();
        return Health.up()
                .withDetail("storagePath", dir.getAbsolutePath())
                .withDetail("usableSpaceMB", freeSpaceBytes / (1024 * 1024))
                .build();
    }
}
