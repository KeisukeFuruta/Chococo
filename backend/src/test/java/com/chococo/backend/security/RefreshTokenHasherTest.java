package com.chococo.backend.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RefreshTokenHasherTest {

    private final RefreshTokenHasher hasher = new RefreshTokenHasher();

    @Test
    void sameInputAlwaysProducesTheSameHash() {
        assertThat(hasher.hash("abc")).isEqualTo(hasher.hash("abc"));
    }

    @Test
    void differentInputsProduceDifferentHashes() {
        assertThat(hasher.hash("abc")).isNotEqualTo(hasher.hash("abd"));
    }

    @Test
    void hashIsA64CharacterHexString_matchingTheTokenHashColumnLength() {
        // database-design.md 2.5節：refresh_tokens.token_hash VARCHAR(64)（SHA-256の16進数表現）
        String hash = hasher.hash("some-raw-refresh-token");
        assertThat(hash).hasSize(64).matches("[0-9a-f]+");
    }
}
