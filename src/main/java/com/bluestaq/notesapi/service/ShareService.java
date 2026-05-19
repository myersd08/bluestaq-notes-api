package com.bluestaq.notesapi.service;

import com.bluestaq.notesapi.dto.ShareRequest;
import com.bluestaq.notesapi.dto.ShareResponse;
import com.bluestaq.notesapi.model.Share;
import com.bluestaq.notesapi.model.User;
import com.bluestaq.notesapi.repository.NoteRepository;
import com.bluestaq.notesapi.repository.ShareRepository;
import com.bluestaq.notesapi.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ShareService {

    private final NoteRepository noteRepository;
    private final ShareRepository shareRepository;
    private final UserRepository userRepository;

    @Transactional
    public ShareResponse share(UUID noteId, ShareRequest request, User caller) {
        var note = noteRepository.findById(noteId)
                .orElseThrow(NoteNotFoundException::new);

        if (!note.getOwnerId().equals(caller.getId())) {
            throw new NoteAccessDeniedException();
        }

        var target = userRepository.findByUsername(request.username())
                .orElseThrow(UserNotFoundException::new);

        if (target.getId().equals(caller.getId())) {
            throw new SelfShareException();
        }

        var existing = shareRepository.findByNoteIdAndSharedWithUserId(noteId, target.getId());
        if (existing.isPresent()) {
            return toResponse(existing.get(), target.getUsername());
        }

        Share share = new Share();
        share.setNoteId(noteId);
        share.setSharedWithUserId(target.getId());
        shareRepository.save(share);

        return toResponse(share, target.getUsername());
    }

    private ShareResponse toResponse(Share share, String sharedWithUsername) {
        return new ShareResponse(share.getNoteId(), sharedWithUsername, share.getCreatedAt());
    }
}
