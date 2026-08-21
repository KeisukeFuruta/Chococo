package com.chococo.backend.dto.pairing;

import java.util.List;

// api-spec.md 3.4節のレスポンス形式。質問文・選択肢はAIがスイーツ名から動的に生成する
public record PairingQuestionsResponse(List<QuestionDto> questions) {

    public record QuestionDto(String question, List<String> options) {
    }
}
