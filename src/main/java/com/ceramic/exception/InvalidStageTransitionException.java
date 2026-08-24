package com.ceramic.exception;

public class InvalidStageTransitionException extends RuntimeException {
    public InvalidStageTransitionException(String message) {
        super(message);
    }
}
