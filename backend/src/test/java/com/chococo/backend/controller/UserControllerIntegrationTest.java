package com.chococo.backend.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.chococo.backend.dto.auth.AuthResponse;
import com.chococo.backend.dto.auth.SignupRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

// api-spec.md 3.13/3.14節を実際のDB・HTTP層を通して検証する
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class UserControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void getCurrentUser_forNewlySignedUpUser_returnsTutorialNotCompleted() throws Exception {
        AuthResponse signedUp = signup("me@example.com", "password123");

        mockMvc.perform(get("/api/users/me").header("Authorization", "Bearer " + signedUp.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("me@example.com"))
                .andExpect(jsonPath("$.tutorialCompleted").value(false));
    }

    @Test
    void getCurrentUser_withoutAuthentication_returns401() throws Exception {
        mockMvc.perform(get("/api/users/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void completeTutorial_marksTutorialCompleted_andIsIdempotent() throws Exception {
        AuthResponse signedUp = signup("tutorial@example.com", "password123");
        String authHeader = "Bearer " + signedUp.token();

        mockMvc.perform(post("/api/users/me/tutorial/complete").header("Authorization", authHeader))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/users/me").header("Authorization", authHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tutorialCompleted").value(true));

        // 2回目の呼び出しも冪等に成功する
        mockMvc.perform(post("/api/users/me/tutorial/complete").header("Authorization", authHeader))
                .andExpect(status().isNoContent());
    }

    private AuthResponse signup(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SignupRequest(email, password))))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readValue(result.getResponse().getContentAsString(), AuthResponse.class);
    }
}
