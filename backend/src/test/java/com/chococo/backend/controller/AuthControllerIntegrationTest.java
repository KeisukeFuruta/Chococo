package com.chococo.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.chococo.backend.dto.auth.AuthResponse;
import com.chococo.backend.dto.auth.LoginRequest;
import com.chococo.backend.dto.auth.RefreshRequest;
import com.chococo.backend.dto.auth.SignupRequest;
import com.chococo.backend.entity.User;
import com.chococo.backend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

// api-spec.md 3.1/3.2/3.10/3.11節、auth-design.md 4章を実際のDB・HTTP層を通して検証する。
// @Transactionalによりテストごとの変更は自動ロールバックされ、共有のdocker-compose DBを汚さない
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void signup_createsUserWithHashedPassword_andReturnsTokenPair() throws Exception {
        mockMvc.perform(signupRequest("new-user@example.com", "password123"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.user.email").value("new-user@example.com"));

        User saved = userRepository.findByEmail("new-user@example.com").orElseThrow();
        assertThat(saved.getPasswordHash()).isNotEqualTo("password123");
        assertThat(passwordEncoder.matches("password123", saved.getPasswordHash())).isTrue();
    }

    @Test
    void signup_withAlreadyRegisteredEmail_returns409() throws Exception {
        mockMvc.perform(signupRequest("dup@example.com", "password123")).andExpect(status().isCreated());

        mockMvc.perform(signupRequest("dup@example.com", "anotherPassword1"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("EMAIL_ALREADY_EXISTS"));
    }

    @Test
    void signup_withInvalidEmailAndTooShortPassword_returns400() throws Exception {
        mockMvc.perform(signupRequest("not-an-email", "short"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void login_withCorrectCredentials_returns200WithTokenPair() throws Exception {
        mockMvc.perform(signupRequest("login@example.com", "password123")).andExpect(status().isCreated());

        mockMvc.perform(loginRequest("login@example.com", "password123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty());
    }

    @Test
    void login_withWrongPassword_returns401WithGenericMessage() throws Exception {
        mockMvc.perform(signupRequest("login2@example.com", "password123")).andExpect(status().isCreated());

        mockMvc.perform(loginRequest("login2@example.com", "wrongPassword1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.error.message").value("メールアドレスまたはパスワードが正しくありません"));
    }

    @Test
    void login_withNonexistentEmail_returnsTheSameErrorAsWrongPassword() throws Exception {
        // auth-design.md 8節：メール不存在とパスワード誤りを区別しない
        mockMvc.perform(loginRequest("no-such-user@example.com", "password123"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    void refresh_rotatesTheToken_andTheOldTokenCanNeverBeReused() throws Exception {
        AuthResponse signedUp = signup("refresh@example.com", "password123");

        MvcResult refreshResult = mockMvc.perform(refreshRequest(signedUp.refreshToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andReturn();

        String newRefreshToken =
                objectMapper.readTree(refreshResult.getResponse().getContentAsString()).get("refreshToken").asString();
        assertThat(newRefreshToken).isNotEqualTo(signedUp.refreshToken());

        // 使用済みの旧トークンは二度と使えない（auth-design.md 4-2節）
        mockMvc.perform(refreshRequest(signedUp.refreshToken()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("REFRESH_TOKEN_INVALID"));
    }

    @Test
    void refresh_withUnknownToken_returns401() throws Exception {
        mockMvc.perform(refreshRequest("this-token-does-not-exist"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("REFRESH_TOKEN_INVALID"));
    }

    @Test
    void logout_revokesTheToken_soASubsequentRefreshFails() throws Exception {
        AuthResponse signedUp = signup("logout@example.com", "password123");

        mockMvc.perform(logoutRequest(signedUp.refreshToken())).andExpect(status().isNoContent());

        mockMvc.perform(refreshRequest(signedUp.refreshToken()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("REFRESH_TOKEN_INVALID"));
    }

    @Test
    void logout_withAnUnknownToken_stillReturns204() throws Exception {
        // api-spec.md 3.11節：フロントは呼び出しの成否によらずローカルのトークンを破棄する想定のため、
        // サーバー側も未知のトークンでエラーにする必要はない
        mockMvc.perform(logoutRequest("never-issued-token")).andExpect(status().isNoContent());
    }

    @Test
    void accessTokenIssuedBySignup_authenticatesProtectedEndpoints() throws Exception {
        AuthResponse signedUp = signup("protected@example.com", "password123");

        // RecordControllerは未実装のため404になるが、401にならないこと自体がJWT認証成功の証跡
        mockMvc.perform(get("/api/records").header("Authorization", "Bearer " + signedUp.token()))
                .andExpect(status().isNotFound());
    }

    private AuthResponse signup(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(signupRequest(email, password))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readValue(result.getResponse().getContentAsString(), AuthResponse.class);
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder signupRequest(
            String email, String password) throws Exception {
        return post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new SignupRequest(email, password)));
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder loginRequest(
            String email, String password) throws Exception {
        return post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest(email, password)));
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder refreshRequest(
            String refreshToken) throws Exception {
        return post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new RefreshRequest(refreshToken)));
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder logoutRequest(
            String refreshToken) throws Exception {
        return post("/api/auth/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new RefreshRequest(refreshToken)));
    }
}
