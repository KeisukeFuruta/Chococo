package com.chococo.backend.dto.auth;

import com.chococo.backend.entity.User;

public record UserDto(Long id, String email) {

    public static UserDto from(User user) {
        return new UserDto(user.getId(), user.getEmail());
    }
}
