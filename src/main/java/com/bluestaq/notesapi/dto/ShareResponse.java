package com.bluestaq.notesapi.dto;

import java.time.Instant;
import java.util.UUID;

public record ShareResponse(UUID noteId, String sharedWithUsername, Instant createdAt) {}
