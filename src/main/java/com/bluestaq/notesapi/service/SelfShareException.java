package com.bluestaq.notesapi.service;

public class SelfShareException extends RuntimeException {
    public SelfShareException() {
        super("Cannot share a note with yourself");
    }
}
