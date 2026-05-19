package com.bluestaq.notesapi.controller;

import com.bluestaq.notesapi.dto.CreateNoteRequest;
import com.bluestaq.notesapi.dto.NoteResponse;
import com.bluestaq.notesapi.dto.ShareRequest;
import com.bluestaq.notesapi.dto.ShareResponse;
import com.bluestaq.notesapi.dto.UpdateNoteRequest;
import com.bluestaq.notesapi.model.User;
import com.bluestaq.notesapi.service.NoteService;
import com.bluestaq.notesapi.service.ShareService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/notes")
@RequiredArgsConstructor
public class NoteController {

    private final NoteService noteService;
    private final ShareService shareService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public NoteResponse create(@Valid @RequestBody CreateNoteRequest request,
                               @AuthenticationPrincipal User caller) {
        return noteService.create(request, caller);
    }

    @GetMapping
    public List<NoteResponse> list(@AuthenticationPrincipal User caller) {
        return noteService.list(caller);
    }

    @GetMapping("/{id}")
    public NoteResponse get(@PathVariable UUID id,
                            @AuthenticationPrincipal User caller) {
        return noteService.get(id, caller);
    }

    @PutMapping("/{id}")
    public NoteResponse update(@PathVariable UUID id,
                               @RequestBody UpdateNoteRequest request,
                               @AuthenticationPrincipal User caller) {
        return noteService.update(id, request, caller);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id,
                       @AuthenticationPrincipal User caller) {
        noteService.delete(id, caller);
    }

    @PostMapping("/{id}/share")
    public ShareResponse share(@PathVariable UUID id,
                               @Valid @RequestBody ShareRequest request,
                               @AuthenticationPrincipal User caller) {
        return shareService.share(id, request, caller);
    }
}
