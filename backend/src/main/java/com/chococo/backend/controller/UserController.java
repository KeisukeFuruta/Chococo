package com.chococo.backend.controller;

import com.chococo.backend.dto.auth.UserDto;
import com.chococo.backend.security.AuthenticatedUser;
import com.chococo.backend.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

// api-spec.md 3.13/3.14節。認証必須（SecurityConfigのanyRequest().authenticated()）
@RestController
@RequestMapping("/api/users/me")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public UserDto getCurrentUser(@AuthenticationPrincipal AuthenticatedUser user) {
        return userService.getCurrentUser(user.id());
    }

    @PostMapping("/tutorial/complete")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void completeTutorial(@AuthenticationPrincipal AuthenticatedUser user) {
        userService.completeTutorial(user.id());
    }
}
