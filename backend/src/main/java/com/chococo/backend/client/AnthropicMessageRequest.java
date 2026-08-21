package com.chococo.backend.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

// Anthropic Messages API（https://api.anthropic.com/v1/messages）のリクエスト形式。
// SDKは導入せず、必要な最小限のフィールドのみをRestClientで直接シリアライズする（tech-stack.md AI API連携）
public record AnthropicMessageRequest(
        String model,
        @JsonProperty("max_tokens") int maxTokens,
        List<AnthropicMessage> messages) {

    public record AnthropicMessage(String role, String content) {
    }
}
