package com.chococo.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.chococo.backend.client.AnthropicClient;
import com.chococo.backend.dto.pairing.PairingSuggestionRequest;
import com.chococo.backend.entity.CoffeeBean;
import com.chococo.backend.entity.PairingSuggestion;
import com.chococo.backend.entity.User;
import com.chococo.backend.exception.AiServiceException;
import com.chococo.backend.repository.CoffeeBeanRepository;
import com.chococo.backend.repository.PairingSuggestionRepository;
import com.chococo.backend.repository.UserRepository;
import com.chococo.backend.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

// api-spec.md 3.3/3.4節を実際のHTTP・Security・DB層を通して検証する。
// AnthropicClientのみ@MockitoBeanで差し替え、実際のAI APIは一切呼び出さない
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PairingControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CoffeeBeanRepository coffeeBeanRepository;

    @Autowired
    private PairingSuggestionRepository pairingSuggestionRepository;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private AnthropicClient anthropicClient;

    @Test
    void usage_forANewUser_returnsZeroUsedAndFullRemaining() throws Exception {
        User user = createUser("usage@example.com");

        mockMvc.perform(get("/api/pairings/usage").header("Authorization", bearerToken(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usedCount").value(0))
                .andExpect(jsonPath("$.limit").value(10))
                .andExpect(jsonPath("$.remainingCount").value(10))
                .andExpect(jsonPath("$.resetAt", endsWith("+09:00")));
    }

    @Test
    void usage_withoutAuthHeader_returns401() throws Exception {
        mockMvc.perform(get("/api/pairings/usage")).andExpect(status().isUnauthorized());
    }

    @Test
    void suggest_withValidAiResponse_savesSuggestion_andReturns201() throws Exception {
        User user = createUser("suggest@example.com");
        CoffeeBean bean = coffeeBeanRepository.findAll().get(0);
        when(anthropicClient.complete(anyString()))
                .thenReturn("{\"coffeeBeanId\": %d, \"reason\": \"よく合います\"}".formatted(bean.getId()));

        mockMvc.perform(suggestRequest(user, "ショートケーキ"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.pairingSuggestionId").isNumber())
                .andExpect(jsonPath("$.sweetName").value("ショートケーキ"))
                .andExpect(jsonPath("$.coffeeBean.id").value(bean.getId()))
                .andExpect(jsonPath("$.reason").value("よく合います"))
                .andExpect(jsonPath("$.remainingCount").value(9));

        assertThat(pairingSuggestionRepository.count()).isEqualTo(1);
    }

    @Test
    void suggest_withBlankSweetName_returns400_andNeverCallsAi() throws Exception {
        User user = createUser("validation@example.com");

        mockMvc.perform(suggestRequest(user, ""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));

        verifyNoInteractions(anthropicClient);
    }

    @Test
    void suggest_whenDailyLimitAlreadyReached_returns429_withoutCallingAi() throws Exception {
        User user = createUser("limit@example.com");
        CoffeeBean bean = coffeeBeanRepository.findAll().get(0);
        for (int i = 0; i < 10; i++) {
            pairingSuggestionRepository.save(PairingSuggestion.builder()
                    .userId(user.getId())
                    .sweetName("既存の提案")
                    .coffeeBean(bean)
                    .reason("理由")
                    .build());
        }

        mockMvc.perform(suggestRequest(user, "プリン"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error.code").value("RATE_LIMIT_EXCEEDED"));

        verifyNoInteractions(anthropicClient);
    }

    @Test
    void suggest_whenAiCallFails_returns502_andDoesNotCountTowardTheDailyLimit() throws Exception {
        User user = createUser("ai-failure@example.com");
        when(anthropicClient.complete(anyString())).thenThrow(new AiServiceException("timeout"));

        mockMvc.perform(suggestRequest(user, "ショートケーキ"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.error.code").value("AI_SERVICE_ERROR"));

        mockMvc.perform(get("/api/pairings/usage").header("Authorization", bearerToken(user)))
                .andExpect(jsonPath("$.usedCount").value(0));
    }

    private MockHttpServletRequestBuilder suggestRequest(User user, String sweetName) throws Exception {
        return post("/api/pairings")
                .header("Authorization", bearerToken(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new PairingSuggestionRequest(sweetName)));
    }

    private String bearerToken(User user) {
        return "Bearer " + jwtService.generateAccessToken(user.getId(), user.getEmail());
    }

    private User createUser(String email) {
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash("irrelevant-hash-for-this-test");
        return userRepository.save(user);
    }
}
