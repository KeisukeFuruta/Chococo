package com.chococo.backend.service;

import com.chococo.backend.dto.auth.UserDto;
import com.chococo.backend.entity.User;
import com.chococo.backend.repository.UserRepository;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserDto getCurrentUser(Long userId) {
        return UserDto.from(userRepository.findById(userId).orElseThrow());
    }

    // チュートリアルのスキップも完了として扱う。何度呼ばれても冪等
    @Transactional
    public void completeTutorial(Long userId) {
        User user = userRepository.findById(userId).orElseThrow();
        if (user.getTutorialCompletedAt() == null) {
            user.setTutorialCompletedAt(Instant.now());
        }
    }
}
