package com.bluestaq.notesapi.service;

import com.bluestaq.notesapi.dto.CreateNoteRequest;
import com.bluestaq.notesapi.dto.NoteResponse;
import com.bluestaq.notesapi.dto.UpdateNoteRequest;
import com.bluestaq.notesapi.model.Note;
import com.bluestaq.notesapi.model.User;
import com.bluestaq.notesapi.repository.NoteRepository;
import com.bluestaq.notesapi.repository.ShareRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NoteService {

    private final NoteRepository noteRepository;
    private final ShareRepository shareRepository;

    @Transactional
    public NoteResponse create(CreateNoteRequest request, User caller) {
        Note note = new Note();
        note.setOwnerId(caller.getId());
        note.setTitle(request.title());
        note.setContent(request.content());
        return toResponse(noteRepository.save(note));
    }

    public List<NoteResponse> list(User caller) {
        return noteRepository.findByOwnerId(caller.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public NoteResponse get(UUID noteId, User caller) {
        Note note = noteRepository.findById(noteId)
                .orElseThrow(NoteNotFoundException::new);
        if (!isOwner(note, caller) && !isSharedWith(note, caller)) {
            throw new NoteAccessDeniedException();
        }
        return toResponse(note);
    }

    @Transactional
    public NoteResponse update(UUID noteId, UpdateNoteRequest request, User caller) {
        if (request.title() == null && request.content() == null) {
            throw new NoUpdateFieldsException();
        }
        Note note = noteRepository.findById(noteId)
                .orElseThrow(NoteNotFoundException::new);
        if (!isOwner(note, caller)) {
            throw new NoteAccessDeniedException();
        }
        if (request.title() != null) {
            note.setTitle(request.title());
        }
        if (request.content() != null) {
            note.setContent(request.content());
        }
        return toResponse(noteRepository.save(note));
    }

    @Transactional
    public void delete(UUID noteId, User caller) {
        Note note = noteRepository.findById(noteId)
                .orElseThrow(NoteNotFoundException::new);
        if (!isOwner(note, caller)) {
            throw new NoteAccessDeniedException();
        }
        noteRepository.delete(note);
    }

    private boolean isOwner(Note note, User caller) {
        return note.getOwnerId().equals(caller.getId());
    }

    private boolean isSharedWith(Note note, User caller) {
        return shareRepository.existsByNoteIdAndSharedWithUserId(note.getId(), caller.getId());
    }

    private NoteResponse toResponse(Note note) {
        return new NoteResponse(
                note.getId(),
                note.getTitle(),
                note.getContent(),
                note.getOwnerId(),
                note.getCreatedAt(),
                note.getUpdatedAt()
        );
    }
}
