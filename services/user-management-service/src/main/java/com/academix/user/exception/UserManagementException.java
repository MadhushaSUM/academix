package com.academix.user.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Base custom exception for user management related errors.
 * This class serves as a superclass for more specific exceptions,
 * allowing for centralized exception handling based on the type of exception.
 */
@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class UserManagementException extends RuntimeException {

    public UserManagementException(String message) {
        super(message);
    }

    public UserManagementException(String message, Throwable cause) {
        super(message, cause);
    }
}