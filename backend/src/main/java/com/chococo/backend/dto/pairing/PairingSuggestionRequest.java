package com.chococo.backend.dto.pairing;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

// api-spec.md 3.5節：sweetNameは1〜100文字。answersは任意（最大2件、質問・回答は1〜200/100文字）
public record PairingSuggestionRequest(
        @NotBlank @Size(max = 100) String sweetName, @Size(max = 2) List<@Valid AnswerDto> answers) {

    public record AnswerDto(
            @NotBlank @Size(max = 200) String question, @NotBlank @Size(max = 100) String answer) {
    }
}
