package com.chococo.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
import org.springframework.http.HttpMethod;
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

    @Test
    void detail_withOwnRecord_returns200WithFullDetails() throws Exception {
        User user = createUser("detail@example.com");
        Record record = recordRepository.save(Record.builder()
                .userId(user.getId())
                .sweetName("ショートケーキ")
                .recordDate(java.time.LocalDate.of(2026, 8, 3))
                .comment("美味しかった")
                .build());

        mockMvc.perform(get("/api/records/{id}", record.getId()).header("Authorization", bearerToken(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(record.getId()))
                .andExpect(jsonPath("$.sweetName").value("ショートケーキ"))
                .andExpect(jsonPath("$.comment").value("美味しかった"));
    }

    @Test
    void detail_withAnotherUsersRecord_returns404() throws Exception {
        User owner = createUser("detail-owner@example.com");
        User other = createUser("detail-other@example.com");
        Record record = recordRepository.save(Record.builder()
                .userId(owner.getId())
                .sweetName("ショートケーキ")
                .recordDate(java.time.LocalDate.of(2026, 8, 3))
                .build());

        mockMvc.perform(get("/api/records/{id}", record.getId()).header("Authorization", bearerToken(other)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
    }

    @Test
    void detail_withNonExistentId_returns404() throws Exception {
        User user = createUser("detail-missing@example.com");

        mockMvc.perform(get("/api/records/{id}", 999_999).header("Authorization", bearerToken(user)))
                .andExpect(status().isNotFound());
    }

    @Test
    void detail_withoutAuthHeader_returns401() throws Exception {
        mockMvc.perform(get("/api/records/{id}", 1)).andExpect(status().isUnauthorized());
    }

    @Test
    void update_changesEditableFields_andReturns200() throws Exception {
        User user = createUser("update@example.com");
        Record record = recordRepository.save(Record.builder()
                .userId(user.getId())
                .sweetName("元の名前")
                .recordDate(java.time.LocalDate.of(2026, 8, 3))
                .comment("元の感想")
                .build());

        mockMvc.perform(updateRequest(user, record.getId())
                        .param("sweetName", "新しい名前")
                        .param("recordDate", "2026-08-10")
                        .param("comment", "新しい感想"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sweetName").value("新しい名前"))
                .andExpect(jsonPath("$.recordDate").value("2026-08-10"))
                .andExpect(jsonPath("$.comment").value("新しい感想"));

        Record updated = recordRepository.findById(record.getId()).orElseThrow();
        assertThat(updated.getSweetName()).isEqualTo("新しい名前");
    }

    @Test
    void update_doesNotAllowChangingThePairingSuggestionSnapshot() throws Exception {
        User user = createUser("update-snapshot@example.com");
        CoffeeBean bean = coffeeBeanRepository.findAll().get(0);
        PairingSuggestion suggestion = pairingSuggestionRepository.save(PairingSuggestion.builder()
                .userId(user.getId())
                .sweetName("モンブラン")
                .coffeeBean(bean)
                .reason("栗の甘さに合います")
                .build());
        Record record = recordRepository.save(Record.builder()
                .userId(user.getId())
                .pairingSuggestionId(suggestion.getId())
                .sweetName("モンブラン")
                .coffeeBeanName(bean.getName())
                .aiReason("栗の甘さに合います")
                .recordDate(java.time.LocalDate.of(2026, 8, 3))
                .build());

        mockMvc.perform(updateRequest(user, record.getId())
                        .param("sweetName", "モンブラン（編集済み）")
                        .param("recordDate", "2026-08-03"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.coffeeBeanName").value(bean.getName()))
                .andExpect(jsonPath("$.aiReason").value("栗の甘さに合います"));
    }

    @Test
    void update_withDeletePhotoTrue_clearsThePhotoUrlInTheResponseAndDb() throws Exception {
        User user = createUser("update-delete-photo@example.com");
        Record record = recordRepository.save(Record.builder()
                .userId(user.getId())
                .sweetName("ショートケーキ")
                .recordDate(java.time.LocalDate.of(2026, 8, 3))
                .photoPath("/uploads/existing.jpg")
                .build());

        mockMvc.perform(updateRequest(user, record.getId())
                        .param("sweetName", "ショートケーキ")
                        .param("recordDate", "2026-08-03")
                        .param("deletePhoto", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.photoUrl").doesNotExist());

        assertThat(recordRepository.findById(record.getId()).orElseThrow().getPhotoPath()).isNull();
    }

    @Test
    void update_withAnotherUsersRecord_returns404_andDoesNotChangeIt() throws Exception {
        User owner = createUser("update-owner@example.com");
        User other = createUser("update-other@example.com");
        Record record = recordRepository.save(Record.builder()
                .userId(owner.getId())
                .sweetName("元の名前")
                .recordDate(java.time.LocalDate.of(2026, 8, 3))
                .build());

        mockMvc.perform(updateRequest(other, record.getId())
                        .param("sweetName", "書き換え")
                        .param("recordDate", "2026-08-03"))
                .andExpect(status().isNotFound());

        assertThat(recordRepository.findById(record.getId()).orElseThrow().getSweetName()).isEqualTo("元の名前");
    }

    @Test
    void update_withBlankSweetName_returns400() throws Exception {
        User user = createUser("update-validation@example.com");
        Record record = recordRepository.save(Record.builder()
                .userId(user.getId())
                .sweetName("元の名前")
                .recordDate(java.time.LocalDate.of(2026, 8, 3))
                .build());

        mockMvc.perform(updateRequest(user, record.getId())
                        .param("sweetName", "")
                        .param("recordDate", "2026-08-03"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void delete_removesTheRecord_andReturns204() throws Exception {
        User user = createUser("delete@example.com");
        Record record = recordRepository.save(Record.builder()
                .userId(user.getId())
                .sweetName("ショートケーキ")
                .recordDate(java.time.LocalDate.of(2026, 8, 3))
                .build());

        mockMvc.perform(delete("/api/records/{id}", record.getId()).header("Authorization", bearerToken(user)))
                .andExpect(status().isNoContent());

        assertThat(recordRepository.findById(record.getId())).isEmpty();
    }

    @Test
    void delete_withAnotherUsersRecord_returns404_andDoesNotDeleteIt() throws Exception {
        User owner = createUser("delete-owner@example.com");
        User other = createUser("delete-other@example.com");
        Record record = recordRepository.save(Record.builder()
                .userId(owner.getId())
                .sweetName("ショートケーキ")
                .recordDate(java.time.LocalDate.of(2026, 8, 3))
                .build());

        mockMvc.perform(delete("/api/records/{id}", record.getId()).header("Authorization", bearerToken(other)))
                .andExpect(status().isNotFound());

        assertThat(recordRepository.findById(record.getId())).isPresent();
    }

    @Test
    void delete_withoutAuthHeader_returns401() throws Exception {
        mockMvc.perform(delete("/api/records/{id}", 1)).andExpect(status().isUnauthorized());
    }

    private MockMultipartHttpServletRequestBuilder updateRequest(User user, Long id) {
        return multipart(HttpMethod.PUT, "/api/records/{id}", id).header("Authorization", bearerToken(user));
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
