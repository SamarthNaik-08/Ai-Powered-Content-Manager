package com.contentmanager.modules.ai.exception;

import com.contentmanager.common.exception.AppException;
import org.springframework.http.HttpStatus;

public class MalformedAIResponseException extends AppException {

    public MalformedAIResponseException(String message) {
        super(message, HttpStatus.BAD_GATEWAY);
    }
}
