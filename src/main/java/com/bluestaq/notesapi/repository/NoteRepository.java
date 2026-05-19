package com.bluestaq.notesapi.repository;

import com.bluestaq.notesapi.model.Note;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface NoteRepository extends JpaRepository<Note, UUID> {
    List<Note> findByOwnerId(UUID ownerId);
}
