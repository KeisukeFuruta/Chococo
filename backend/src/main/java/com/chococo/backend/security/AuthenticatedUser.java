package com.chococo.backend.security;

// JWTのsub（ユーザーID）・emailクレームから復元した、SecurityContext上のprincipal。
// UserDetailsServiceは導入しない方針（ステートレスJWT前提でSpring Securityのprovider機構は使わない）ため、
// この軽量recordをそのままprincipalとして使う
public record AuthenticatedUser(Long id, String email) {
}
