package com.chococo.backend.dto.pairing;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// api-spec.md 3.4節：sweetNameは1〜100文字
public record PairingSuggestionRequest(@NotBlank @Size(max = 100) String sweetName) {
}
