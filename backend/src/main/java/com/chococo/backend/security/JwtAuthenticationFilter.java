package com.chococo.backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

// TODO(#8 ユーザー認証イシュー): auth-design.md 5節の検証フローを実装する。
// - Authorization: Bearer <token> ヘッダーを読み取り、署名・有効期限を検証する
// - 検証成功時はemail claimからユーザーをロードしSecurityContextにセットする
// - 検証失敗時（トークンなし・期限切れ・改ざん・ユーザー不在等）は例外を投げず、
//   SecurityContextを未設定のまま次のフィルタへ処理を渡す（一律401はCustomAuthenticationEntryPointが担当）
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        filterChain.doFilter(request, response);
    }
}
