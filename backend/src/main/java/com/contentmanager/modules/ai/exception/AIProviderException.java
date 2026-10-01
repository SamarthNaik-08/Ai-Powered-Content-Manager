package com.contentmanager.modules.ai.exception;

import com.contentmanager.common.exception.AppException;
import org.springframework.http.HttpStatus;

public class AIProviderException extends AppException {

    public AIProviderException(String message) {
        super(message, HttpStatus.BAD_GATEWAY);
    }

    public AIProviderException(String message, HttpStatus status) {
        super(message, status);
    }
}
