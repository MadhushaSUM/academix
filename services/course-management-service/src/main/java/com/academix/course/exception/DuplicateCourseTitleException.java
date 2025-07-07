package com.academix.course.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class DuplicateCourseTitleException extends RuntimeException {
    public DuplicateCourseTitleException(String message) {
        super(message);
    }

    public DuplicateCourseTitleException(String message, Throwable cause) {
        super(message, cause);
    }
}