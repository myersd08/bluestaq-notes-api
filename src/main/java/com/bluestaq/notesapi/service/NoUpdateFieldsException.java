package com.bluestaq.notesapi.service;

public class NoUpdateFieldsException extends RuntimeException {
    public NoUpdateFieldsException() {
        super("At least one field (title or content) must be provided");
    }
}
