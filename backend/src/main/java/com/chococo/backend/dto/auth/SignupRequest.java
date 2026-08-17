package com.chococo.backend.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// api-spec.md 3.1節：passwordは8文字以上72文字以下（BCryptの仕様上の上限。auth-design.md 7節）
public record SignupRequest(
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8, max = 72) String password) {
}
