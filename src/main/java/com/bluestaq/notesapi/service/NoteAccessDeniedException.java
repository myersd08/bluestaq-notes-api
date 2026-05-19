package com.bluestaq.notesapi.service;

public class NoteAccessDeniedException extends RuntimeException {
    public NoteAccessDeniedException() {
        super("Access denied");
    }
}
