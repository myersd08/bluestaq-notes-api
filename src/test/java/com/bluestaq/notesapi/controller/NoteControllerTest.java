package com.bluestaq.notesapi.controller;

import com.bluestaq.notesapi.model.Share;
import com.bluestaq.notesapi.repository.NoteRepository;
import com.bluestaq.notesapi.repository.ShareRepository;
import com.bluestaq.notesapi.repository.UserRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class NoteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NoteRepository noteRepository;

    @Autowired
    private ShareRepository shareRepository;

    @BeforeEach
    void setUp() {
        shareRepository.deleteAll();
        noteRepository.deleteAll();
        userRepository.deleteAll();
    }

    // --- helpers ---

    private String registerAndLogin(String username) throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"pass123!"}
                                """.formatted(username)))
                .andExpect(status().isCreated());

        String body = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"pass123!"}
                                """.formatted(username)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        return JsonPath.read(body, "$.token");
    }

    private String createNote(String token, String title, String content) throws Exception {
        String body = mockMvc.perform(post("/notes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token)
                        .content("""
                                {"title":"%s","content":"%s"}
                                """.formatted(title, content)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return JsonPath.read(body, "$.id");
    }

    // --- test scenarios ---

    @Test
    void createNote_validRequest_returns201WithCorrectOwner() throws Exception {
        String token = registerAndLogin("alice");
        UUID aliceId = userRepository.findByUsername("alice").orElseThrow().getId();

        mockMvc.perform(post("/notes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token)
                        .content("""
                                {"title":"Shopping list","content":"Milk, eggs, bread"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.title").value("Shopping list"))
                .andExpect(jsonPath("$.content").value("Milk, eggs, bread"))
                .andExpect(jsonPath("$.ownerId").value(aliceId.toString()))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.updatedAt").isNotEmpty());

        assertThat(noteRepository.findByOwnerId(aliceId)).hasSize(1);
    }

    @Test
    void listNotes_returnsOnlyCallerOwnedNotes() throws Exception {
        String aliceToken = registerAndLogin("alice");
        String bobToken = registerAndLogin("bob");

        createNote(aliceToken, "Alice note 1", "content A1");
        createNote(aliceToken, "Alice note 2", "content A2");
        createNote(bobToken, "Bob note", "content B");

        mockMvc.perform(get("/notes")
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].title").value("Alice note 1"))
                .andExpect(jsonPath("$[1].title").value("Alice note 2"));
    }

    @Test
    void getNote_ownNote_returns200() throws Exception {
        String token = registerAndLogin("alice");
        String noteId = createNote(token, "My note", "some content");

        mockMvc.perform(get("/notes/" + noteId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(noteId))
                .andExpect(jsonPath("$.content").value("some content"));
    }

    @Test
    void getNote_sharedWithCaller_returns200() throws Exception {
        String aliceToken = registerAndLogin("alice");
        String bobToken = registerAndLogin("bob");
        String noteId = createNote(aliceToken, "Shared note", "shared content");

        UUID bobId = userRepository.findByUsername("bob").orElseThrow().getId();
        Share share = new Share();
        share.setNoteId(UUID.fromString(noteId));
        share.setSharedWithUserId(bobId);
        shareRepository.save(share);

        mockMvc.perform(get("/notes/" + noteId)
                        .header("Authorization", "Bearer " + bobToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(noteId));
    }

    @Test
    void getNote_notSharedWithCaller_returns403() throws Exception {
        String aliceToken = registerAndLogin("alice");
        String bobToken = registerAndLogin("bob");
        String noteId = createNote(aliceToken, "Private note", "private content");

        mockMvc.perform(get("/notes/" + noteId)
                        .header("Authorization", "Bearer " + bobToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void getNote_nonExistent_returns404() throws Exception {
        String token = registerAndLogin("alice");

        mockMvc.perform(get("/notes/" + UUID.randomUUID())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void updateNote_ownNote_returns200WithChangedUpdatedAt() throws Exception {
        String token = registerAndLogin("alice");
        String noteId = createNote(token, "Original title", "original content");

        String beforeBody = mockMvc.perform(get("/notes/" + noteId)
                        .header("Authorization", "Bearer " + token))
                .andReturn().getResponse().getContentAsString();
        String updatedAtBefore = JsonPath.read(beforeBody, "$.updatedAt");

        Thread.sleep(10);

        mockMvc.perform(put("/notes/" + noteId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token)
                        .content("""
                                {"title":"Updated title","content":"updated content"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated title"))
                .andExpect(jsonPath("$.content").value("updated content"));

        String afterBody = mockMvc.perform(get("/notes/" + noteId)
                        .header("Authorization", "Bearer " + token))
                .andReturn().getResponse().getContentAsString();
        String updatedAtAfter = JsonPath.read(afterBody, "$.updatedAt");

        assertThat(updatedAtAfter).isNotEqualTo(updatedAtBefore);
    }

    @Test
    void updateNote_notOwner_returns403() throws Exception {
        String aliceToken = registerAndLogin("alice");
        String bobToken = registerAndLogin("bob");
        String noteId = createNote(aliceToken, "Alice's note", "content");

        mockMvc.perform(put("/notes/" + noteId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + bobToken)
                        .content("""
                                {"content":"hacked"}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void deleteNote_ownNote_returns204AndRemovesShares() throws Exception {
        String aliceToken = registerAndLogin("alice");
        String bobToken = registerAndLogin("bob");
        String noteId = createNote(aliceToken, "To delete", "content");

        UUID bobId = userRepository.findByUsername("bob").orElseThrow().getId();
        Share share = new Share();
        share.setNoteId(UUID.fromString(noteId));
        share.setSharedWithUserId(bobId);
        shareRepository.save(share);

        assertThat(shareRepository.count()).isEqualTo(1);

        mockMvc.perform(delete("/notes/" + noteId)
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isNoContent());

        assertThat(noteRepository.findById(UUID.fromString(noteId))).isEmpty();
        assertThat(shareRepository.count()).isEqualTo(0);
    }

    @Test
    void deleteNote_notOwner_returns403() throws Exception {
        String aliceToken = registerAndLogin("alice");
        String bobToken = registerAndLogin("bob");
        String noteId = createNote(aliceToken, "Alice's note", "content");

        mockMvc.perform(delete("/notes/" + noteId)
                        .header("Authorization", "Bearer " + bobToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));

        assertThat(noteRepository.findById(UUID.fromString(noteId))).isPresent();
    }
}
