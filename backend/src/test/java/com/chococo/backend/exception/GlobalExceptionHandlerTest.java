package com.chococo.backend.exception;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolationException;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.resource.NoResourceFoundException;

// api-spec.md 1.2節のステータスコード・エラーコード対応表を1件ずつ固定する。
// ここが崩れると、フロントエンドのエラーハンドリング（画面ごとの文言出し分け等）が静かに壊れる
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void emailAlreadyExists_mapsTo409() {
        assertMapping(new EmailAlreadyExistsException(), HttpStatus.CONFLICT, "EMAIL_ALREADY_EXISTS");
    }

    @Test
    void invalidCredentials_mapsTo401_withoutRevealingWhichFieldWasWrong() {
        ResponseEntity<ErrorResponse> response = handler.handleApiException(new InvalidCredentialsException());
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody().error().code()).isEqualTo("INVALID_CREDENTIALS");
        assertThat(response.getBody().error().message()).doesNotContain("email", "password");
    }

    @Test
    void refreshTokenInvalid_mapsTo401() {
        assertMapping(new RefreshTokenInvalidException(), HttpStatus.UNAUTHORIZED, "REFRESH_TOKEN_INVALID");
    }

    @Test
    void recordNotFound_mapsTo404_withGenericNotFoundCode() {
        // auth-design.md 10節：他人の記録である場合と存在しない場合を区別しないため、共通のNOT_FOUNDコードを使う
        assertMapping(new RecordNotFoundException(), HttpStatus.NOT_FOUND, "NOT_FOUND");
    }

    @Test
    void pairingSuggestionNotFound_mapsTo404_withSameGenericNotFoundCodeAsRecord() {
        assertMapping(new PairingSuggestionNotFoundException(), HttpStatus.NOT_FOUND, "NOT_FOUND");
    }

    @Test
    void pairingSuggestionAlreadyUsed_mapsTo409() {
        assertMapping(
                new PairingSuggestionAlreadyUsedException(), HttpStatus.CONFLICT, "PAIRING_SUGGESTION_ALREADY_USED");
    }

    @Test
    void rateLimitExceeded_mapsTo429() {
        assertMapping(new RateLimitExceededException(), HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMIT_EXCEEDED");
    }

    @Test
    void aiServiceError_mapsTo502() {
        assertMapping(new AiServiceException("timeout"), HttpStatus.BAD_GATEWAY, "AI_SERVICE_ERROR");
    }

    @Test
    void constraintViolation_mapsTo400VALIDATION_ERROR() {
        ResponseEntity<ErrorResponse> response =
                handler.handleValidationException(new ConstraintViolationException(Set.of()));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().error().code()).isEqualTo("VALIDATION_ERROR");
    }

    @Test
    void noResourceFound_mapsTo404_ratherThanBeingSwallowedByTheCatchAll500Handler() {
        // 実機確認で見つかった不具合の再発防止（GlobalExceptionHandlerのException.class catch-allが
        // Spring自身のNoResourceFoundExceptionまで飲み込み、本来404であるべきものが500になっていた）
        ResponseEntity<ErrorResponse> response = handler.handleNoResourceFoundException(
                new NoResourceFoundException(HttpMethod.GET, "/uploads/missing.jpg", "No static resource found"));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().error().code()).isEqualTo("NOT_FOUND");
    }

    @Test
    void unexpectedException_mapsTo500_withoutLeakingInternalDetails() {
        ResponseEntity<ErrorResponse> response =
                handler.handleUnexpectedException(new IllegalStateException("db connection pool exhausted"));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().error().code()).isEqualTo("INTERNAL_ERROR");
        assertThat(response.getBody().error().message()).doesNotContain("db connection pool exhausted");
    }

    private void assertMapping(ApiException exception, HttpStatus expectedStatus, String expectedCode) {
        ResponseEntity<ErrorResponse> response = handler.handleApiException(exception);
        assertThat(response.getStatusCode()).isEqualTo(expectedStatus);
        assertThat(response.getBody().error().code()).isEqualTo(expectedCode);
    }
}
