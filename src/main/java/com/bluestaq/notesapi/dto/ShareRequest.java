package com.bluestaq.notesapi.dto;

import jakarta.validation.constraints.NotBlank;

public record ShareRequest(@NotBlank String username) {}
