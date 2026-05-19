package com.bluestaq.notesapi.controller;

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
class NoteShareControllerTest {

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

    private void shareNote(String ownerToken, String noteId, String targetUsername) throws Exception {
        mockMvc.perform(post("/notes/" + noteId + "/share")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + ownerToken)
                        .content("""
                                {"username":"%s"}
                                """.formatted(targetUsername)))
                .andExpect(status().isOk());
    }

    // --- Scenario 1: Owner shares note with a valid user → 200, share persisted ---

    @Test
    void shareNote_ownerSharesWithValidUser_returns200AndSharePersisted() throws Exception {
        String aliceToken = registerAndLogin("alice");
        registerAndLogin("bob");
        String noteId = createNote(aliceToken, "Alice's note", "content");

        mockMvc.perform(post("/notes/" + noteId + "/share")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + aliceToken)
                        .content("""
                                {"username":"bob"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.noteId").value(noteId))
                .andExpect(jsonPath("$.sharedWithUsername").value("bob"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty());

        assertThat(shareRepository.count()).isEqualTo(1);
    }

    // --- Scenario 2: Owner shares again with same user → 200, no duplicate ---

    @Test
    void shareNote_ownerSharesAgainWithSameUser_returns200AndNoDuplicate() throws Exception {
        String aliceToken = registerAndLogin("alice");
        registerAndLogin("bob");
        String noteId = createNote(aliceToken, "Alice's note", "content");

        shareNote(aliceToken, noteId, "bob");
        assertThat(shareRepository.count()).isEqualTo(1);

        mockMvc.perform(post("/notes/" + noteId + "/share")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + aliceToken)
                        .content("""
                                {"username":"bob"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sharedWithUsername").value("bob"));

        assertThat(shareRepository.count()).isEqualTo(1);
    }

    // --- Scenario 3: Non-owner attempts to share → 403 ---

    @Test
    void shareNote_nonOwnerAttempsShare_returns403() throws Exception {
        String aliceToken = registerAndLogin("alice");
        String bobToken = registerAndLogin("bob");
        registerAndLogin("charlie");
        String noteId = createNote(aliceToken, "Alice's note", "content");

        mockMvc.perform(post("/notes/" + noteId + "/share")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + bobToken)
                        .content("""
                                {"username":"charlie"}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));

        assertThat(shareRepository.count()).isEqualTo(0);
    }

    // --- Scenario 4: Share with non-existent username → 404 ---

    @Test
    void shareNote_targetUserDoesNotExist_returns404() throws Exception {
        String aliceToken = registerAndLogin("alice");
        String noteId = createNote(aliceToken, "Alice's note", "content");

        mockMvc.perform(post("/notes/" + noteId + "/share")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + aliceToken)
                        .content("""
                                {"username":"nobody"}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    // --- Scenario 5: Owner shares note with themselves → 400 ---

    @Test
    void shareNote_ownerSharesWithThemselves_returns400() throws Exception {
        String aliceToken = registerAndLogin("alice");
        String noteId = createNote(aliceToken, "Alice's note", "content");

        mockMvc.perform(post("/notes/" + noteId + "/share")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + aliceToken)
                        .content("""
                                {"username":"alice"}
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422));

        assertThat(shareRepository.count()).isEqualTo(0);
    }

    // --- Scenario 6: Shared recipient calls GET /notes/{id} → 200 ---

    @Test
    void getNote_sharedRecipient_returns200() throws Exception {
        String aliceToken = registerAndLogin("alice");
        String bobToken = registerAndLogin("bob");
        String noteId = createNote(aliceToken, "Shared note", "shared content");

        shareNote(aliceToken, noteId, "bob");

        mockMvc.perform(get("/notes/" + noteId)
                        .header("Authorization", "Bearer " + bobToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(noteId))
                .andExpect(jsonPath("$.content").value("shared content"));
    }

    // --- Scenario 7: Shared recipient calls PUT /notes/{id} → 403 ---

    @Test
    void updateNote_sharedRecipient_returns403() throws Exception {
        String aliceToken = registerAndLogin("alice");
        String bobToken = registerAndLogin("bob");
        String noteId = createNote(aliceToken, "Alice's note", "original content");

        shareNote(aliceToken, noteId, "bob");

        mockMvc.perform(put("/notes/" + noteId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + bobToken)
                        .content("""
                                {"content":"modified content"}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    // --- Scenario 8: Shared recipient calls DELETE /notes/{id} → 403 ---

    @Test
    void deleteNote_sharedRecipient_returns403() throws Exception {
        String aliceToken = registerAndLogin("alice");
        String bobToken = registerAndLogin("bob");
        String noteId = createNote(aliceToken, "Alice's note", "content");

        shareNote(aliceToken, noteId, "bob");

        mockMvc.perform(delete("/notes/" + noteId)
                        .header("Authorization", "Bearer " + bobToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));

        assertThat(noteRepository.findById(UUID.fromString(noteId))).isPresent();
    }
}
