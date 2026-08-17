package com.chococo.backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

// auth-design.md 5節：Authorization: Bearer <token> を検証し、成功時のみSecurityContextに認証情報をセットする。
// 検証失敗時（トークンなし・期限切れ・改ざん・不正形式）は例外を投げず、SecurityContextを未設定のまま次へ進める。
// 「トークンが無い場合」と「トークンが不正な場合」を同じ経路で一律401（CustomAuthenticationEntryPoint）として扱うため
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        extractToken(request)
                .flatMap(jwtService::parseAccessToken)
                .ifPresent(this::setAuthentication);
        filterChain.doFilter(request, response);
    }

    private Optional<String> extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            return Optional.of(header.substring(BEARER_PREFIX.length()));
        }
        return Optional.empty();
    }

    private void setAuthentication(AuthenticatedUser authenticatedUser) {
        var authentication =
                new UsernamePasswordAuthenticationToken(authenticatedUser, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
