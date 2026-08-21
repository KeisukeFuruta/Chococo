package com.chococo.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.chococo.backend.entity.CoffeeBean;
import com.chococo.backend.entity.PairingSuggestion;
import com.chococo.backend.entity.Record;
import com.chococo.backend.entity.User;
import com.chococo.backend.repository.CoffeeBeanRepository;
import com.chococo.backend.repository.PairingSuggestionRepository;
import com.chococo.backend.repository.RecordRepository;
import com.chococo.backend.repository.UserRepository;
import com.chococo.backend.security.JwtService;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

// api-spec.md 3.5/3.6節を実際のHTTP・Security・DB層を通して検証する
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RecordControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CoffeeBeanRepository coffeeBeanRepository;

    @Autowired
    private PairingSuggestionRepository pairingSuggestionRepository;

    @Autowired
    private RecordRepository recordRepository;

    @Autowired
    private JwtService jwtService;

    @Test
    void create_withMinimumRequiredFields_savesRecord_andReturns201() throws Exception {
        User user = createUser("create@example.com");

        mockMvc.perform(createRequest(user)
                        .param("sweetName", "ショートケーキ")
                        .param("recordDate", "2026-08-03"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.sweetName").value("ショートケーキ"))
                .andExpect(jsonPath("$.recordDate").value("2026-08-03"))
                .andExpect(jsonPath("$.photoUrl").doesNotExist())
                .andExpect(jsonPath("$.coffeeBeanName").doesNotExist())
                .andExpect(jsonPath("$.createdAt", org.hamcrest.Matchers.endsWith("+09:00")));

        assertThat(recordRepository.count()).isEqualTo(1);
    }

    @Test
    void create_withoutAuthHeader_returns401() throws Exception {
        mockMvc.perform(multipart("/api/records")
                        .param("sweetName", "ショートケーキ")
                        .param("recordDate", "2026-08-03"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void create_withBlankSweetName_returns400() throws Exception {
        User user = createUser("validation@example.com");

        mockMvc.perform(createRequest(user)
                        .param("sweetName", "")
                        .param("recordDate", "2026-08-03"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void create_withOwnPairingSuggestion_copiesCoffeeBeanNameAndAiReason() throws Exception {
        User user = createUser("pairing@example.com");
        CoffeeBean bean = coffeeBeanRepository.findAll().get(0);
        PairingSuggestion suggestion = pairingSuggestionRepository.save(PairingSuggestion.builder()
                .userId(user.getId())
                .sweetName("モンブラン")
                .coffeeBean(bean)
                .reason("栗の甘さに合います")
                .build());

        mockMvc.perform(createRequest(user)
                        .param("sweetName", "モンブラン")
                        .param("recordDate", "2026-08-03")
                        .param("pairingSuggestionId", suggestion.getId().toString()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.coffeeBeanName").value(bean.getName()))
                .andExpect(jsonPath("$.aiReason").value("栗の甘さに合います"));
    }

    @Test
    void create_withAnotherUsersPairingSuggestion_returns404() throws Exception {
        User owner = createUser("owner@example.com");
        User other = createUser("other@example.com");
        CoffeeBean bean = coffeeBeanRepository.findAll().get(0);
        PairingSuggestion suggestion = pairingSuggestionRepository.save(PairingSuggestion.builder()
                .userId(owner.getId())
                .sweetName("モンブラン")
                .coffeeBean(bean)
                .reason("栗の甘さに合います")
                .build());

        mockMvc.perform(createRequest(other)
                        .param("sweetName", "モンブラン")
                        .param("recordDate", "2026-08-03")
                        .param("pairingSuggestionId", suggestion.getId().toString()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
    }

    @Test
    void create_withAlreadyUsedPairingSuggestion_returns409() throws Exception {
        User user = createUser("used@example.com");
        CoffeeBean bean = coffeeBeanRepository.findAll().get(0);
        PairingSuggestion suggestion = pairingSuggestionRepository.save(PairingSuggestion.builder()
                .userId(user.getId())
                .sweetName("モンブラン")
                .coffeeBean(bean)
                .reason("栗の甘さに合います")
                .build());
        recordRepository.save(Record.builder()
                .userId(user.getId())
                .pairingSuggestionId(suggestion.getId())
                .sweetName("モンブラン")
                .recordDate(java.time.LocalDate.of(2026, 8, 1))
                .build());

        mockMvc.perform(createRequest(user)
                        .param("sweetName", "モンブラン")
                        .param("recordDate", "2026-08-03")
                        .param("pairingSuggestionId", suggestion.getId().toString()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("PAIRING_SUGGESTION_ALREADY_USED"));
    }

    @Test
    void create_withJpgPhoto_savesAndReturnsUploadsPhotoUrl() throws Exception {
        User user = createUser("photo@example.com");
        MockMultipartFile photo =
                new MockMultipartFile("photo", "cake.jpg", "image/jpeg", "fake-image-bytes".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(createRequest(user)
                        .file(photo)
                        .param("sweetName", "ショートケーキ")
                        .param("recordDate", "2026-08-03"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.photoUrl", org.hamcrest.Matchers.startsWith("/uploads/")))
                .andExpect(jsonPath("$.photoUrl", org.hamcrest.Matchers.endsWith(".jpg")));
    }

    @Test
    void create_withNonImagePhoto_returns400() throws Exception {
        User user = createUser("badphoto@example.com");
        MockMultipartFile photo =
                new MockMultipartFile("photo", "note.txt", "text/plain", "not an image".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(createRequest(user)
                        .file(photo)
                        .param("sweetName", "ショートケーキ")
                        .param("recordDate", "2026-08-03"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void list_returnsOnlyTheAuthenticatedUsersRecordsForTheRequestedMonth() throws Exception {
        User user = createUser("list@example.com");
        User other = createUser("list-other@example.com");
        recordRepository.save(Record.builder()
                .userId(user.getId())
                .sweetName("8月の記録")
                .recordDate(java.time.LocalDate.of(2026, 8, 3))
                .build());
        recordRepository.save(Record.builder()
                .userId(user.getId())
                .sweetName("7月の記録")
                .recordDate(java.time.LocalDate.of(2026, 7, 31))
                .build());
        recordRepository.save(Record.builder()
                .userId(other.getId())
                .sweetName("他人の記録")
                .recordDate(java.time.LocalDate.of(2026, 8, 3))
                .build());

        mockMvc.perform(get("/api/records")
                        .header("Authorization", bearerToken(user))
                        .param("year", "2026")
                        .param("month", "8"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.records.length()").value(1))
                .andExpect(jsonPath("$.records[0].sweetName").value("8月の記録"));
    }

    @Test
    void list_forMonthWithNoRecords_returnsEmptyArray() throws Exception {
        User user = createUser("empty-month@example.com");

        mockMvc.perform(get("/api/records")
                        .header("Authorization", bearerToken(user))
                        .param("year", "2099")
                        .param("month", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.records.length()").value(0));
    }

    @Test
    void list_withoutAuthHeader_returns401() throws Exception {
        mockMvc.perform(get("/api/records").param("year", "2026").param("month", "8"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void list_withMonthOutOfRange_returns400() throws Exception {
        User user = createUser("invalid-month@example.com");

        mockMvc.perform(get("/api/records")
                        .header("Authorization", bearerToken(user))
                        .param("year", "2026")
                        .param("month", "13"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    private MockMultipartHttpServletRequestBuilder createRequest(User user) {
        return multipart("/api/records").header("Authorization", bearerToken(user));
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
