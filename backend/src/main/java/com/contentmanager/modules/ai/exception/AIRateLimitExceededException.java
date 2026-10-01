package com.contentmanager.modules.ai.exception;

import com.contentmanager.common.exception.AppException;
import org.springframework.http.HttpStatus;

public class AIRateLimitExceededException extends AppException {

    public AIRateLimitExceededException(String message) {
        super(message, HttpStatus.TOO_MANY_REQUESTS);
    }
}
