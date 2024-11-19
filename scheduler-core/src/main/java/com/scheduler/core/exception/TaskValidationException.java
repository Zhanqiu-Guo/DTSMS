package com.scheduler.core.exception;

public class TaskValidationException extends RuntimeException {
    public TaskValidationException(String message) {
        super(message);
    }
}