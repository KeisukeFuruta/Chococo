package com.chococo.backend.client;

import com.chococo.backend.exception.AiServiceException;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

// tech-stack.md AI API連携：追加SDKは導入せず、Spring RestClientでAnthropic Messages APIを直接HTTP呼び出しする
@Component
public class AnthropicClient {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(20);

    private final RestClient restClient;
    private final String model;
    private final int maxTokens;

    public AnthropicClient(
            @Value("${chococo.ai.anthropic.base-url}") String baseUrl,
            @Value("${chococo.ai.anthropic.api-key}") String apiKey,
            @Value("${chococo.ai.anthropic.model}") String model,
            @Value("${chococo.ai.anthropic.max-tokens}") int maxTokens) {
        this.model = model;
        this.maxTokens = maxTokens;

        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT).build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(READ_TIMEOUT);

        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .defaultHeader("x-api-key", apiKey)
                .defaultHeader("anthropic-version", "2023-06-01")
                .build();
    }

    // 応答本文（content[0].text）をそのまま返す。呼び出し失敗・タイムアウト・空応答は
    // すべてAiServiceException（502 AI_SERVICE_ERROR）に正規化する（api-spec.md 1.2節）
    public String complete(String userPrompt) {
        AnthropicMessageRequest request = new AnthropicMessageRequest(
                model, maxTokens, List.of(new AnthropicMessageRequest.AnthropicMessage("user", userPrompt)));

        AnthropicMessageResponse response;
        try {
            response = restClient.post()
                    .uri("/v1/messages")
                    .body(request)
                    .retrieve()
                    .body(AnthropicMessageResponse.class);
        } catch (RestClientException e) {
            throw new AiServiceException("AI APIの呼び出しに失敗しました", e);
        }

        if (response == null || response.content() == null || response.content().isEmpty()) {
            throw new AiServiceException("AIからの応答が空でした");
        }

        String text = response.content().get(0).text();
        if (text == null || text.isBlank()) {
            throw new AiServiceException("AIからの応答が空でした");
        }
        return text;
    }
}
