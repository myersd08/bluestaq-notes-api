package com.bluestaq.notesapi.repository;

import com.bluestaq.notesapi.model.Share;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ShareRepository extends JpaRepository<Share, UUID> {
    boolean existsByNoteIdAndSharedWithUserId(UUID noteId, UUID sharedWithUserId);
    Optional<Share> findByNoteIdAndSharedWithUserId(UUID noteId, UUID sharedWithUserId);
}
