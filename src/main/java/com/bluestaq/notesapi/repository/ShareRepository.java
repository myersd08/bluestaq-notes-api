package com.bluestaq.notesapi.repository;

import com.bluestaq.notesapi.model.Share;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ShareRepository extends JpaRepository<Share, UUID> {
    boolean existsByNoteIdAndSharedWithUserId(UUID noteId, UUID sharedWithUserId);
}
