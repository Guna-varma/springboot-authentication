package com.app.backend.util;

public class TextEntryNotFoundException extends RuntimeException {

    public TextEntryNotFoundException(String message) {
        super(message);
    }

    public TextEntryNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
