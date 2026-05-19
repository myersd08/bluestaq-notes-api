package com.bluestaq.notesapi.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateNoteRequest(String title, @NotBlank(message = "content is required") String content) {}
