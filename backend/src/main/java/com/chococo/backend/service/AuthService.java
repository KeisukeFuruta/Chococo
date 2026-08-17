package com.chococo.backend.service;

import com.chococo.backend.dto.auth.AuthResponse;
import com.chococo.backend.dto.auth.LoginRequest;
import com.chococo.backend.dto.auth.SignupRequest;
import com.chococo.backend.dto.auth.TokenRefreshResponse;
import com.chococo.backend.dto.auth.UserDto;
import com.chococo.backend.entity.User;
import com.chococo.backend.exception.EmailAlreadyExistsException;
import com.chococo.backend.exception.InvalidCredentialsException;
import com.chococo.backend.exception.RefreshTokenInvalidException;
import com.chococo.backend.repository.UserRepository;
import com.chococo.backend.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// auth-design.md 4章：signup/login/refresh/logoutの業務ロジック。
// UserDetailsService/AuthenticationManagerは使わず、ここで直接パスワード照合・トークン発行を行う
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            RefreshTokenService refreshTokenService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
    }

    @Transactional
    public AuthResponse signup(SignupRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyExistsException();
        }

        User user = new User();
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        userRepository.save(user);

        return issueAuthResponse(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            // auth-design.md 8節：メール不存在・パスワード誤りのいずれも同じ例外・同じ文言にする
            throw new InvalidCredentialsException();
        }

        return issueAuthResponse(user);
    }

    @Transactional
    public TokenRefreshResponse refresh(String rawRefreshToken) {
        RefreshTokenService.RotationResult rotation = refreshTokenService.rotate(rawRefreshToken);
        User user = userRepository.findById(rotation.userId())
                .orElseThrow(RefreshTokenInvalidException::new);

        String accessToken = jwtService.generateAccessToken(user.getId(), user.getEmail());
        return new TokenRefreshResponse(accessToken, rotation.rawToken());
    }

    public void logout(String rawRefreshToken) {
        refreshTokenService.revoke(rawRefreshToken);
    }

    private AuthResponse issueAuthResponse(User user) {
        String accessToken = jwtService.generateAccessToken(user.getId(), user.getEmail());
        String refreshToken = refreshTokenService.issue(user.getId());
        return new AuthResponse(accessToken, refreshToken, UserDto.from(user));
    }
}
