package com.chococo.backend.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

// SecurityConfig（authorizeHttpRequestsのルーティング）とGlobalExceptionHandler/CustomAuthenticationEntryPoint
// （エラーレスポンスの整形）が実際のフィルタチェーンを通して連携することを確認する。
// /uploads/**を実際にcurlで叩くまで気づかなかった「NoResourceFoundExceptionが500に飲み込まれる」不具合の再発防止
@SpringBootTest
@AutoConfigureMockMvc
class SecurityFilterChainIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void protectedEndpoint_withoutAuthHeader_returns401WithSharedErrorBody() throws Exception {
        mockMvc.perform(get("/api/records"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    void authRoute_isPermitAll_soAnUnmappedSubPathSurfacesAs404NotAs401() throws Exception {
        // /api/auth/**の実エンドポイントはAuthController（#8）で実装済みだが、その配下の
        // マッピングされていないパスを叩くことで「401ではなく404になる」＝permitAllであることを検証できる
        // （もしpermitAllが外れれば認証必須になり401に変わり、このテストが落ちる）
        mockMvc.perform(post("/api/auth/no-such-endpoint"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
    }

    @Test
    void uploadsPath_isPermitAll_soAMissingFileSurfacesAs404NotAs401() throws Exception {
        // aws-infra-design.md 3.4節：画像配信はJWT認証を経由しない設計
        mockMvc.perform(get("/uploads/does-not-exist.jpg"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
    }
}
