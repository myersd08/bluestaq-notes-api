package com.bluestaq.notesapi.dto;

public record AuthResponse(String token, long expiresIn) {}
