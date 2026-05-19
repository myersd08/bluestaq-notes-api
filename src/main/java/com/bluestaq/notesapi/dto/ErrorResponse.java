package com.bluestaq.notesapi.dto;

public record ErrorResponse(int status, String error, String message) {}
