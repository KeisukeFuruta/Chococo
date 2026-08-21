package com.chococo.backend.controller;

import com.chococo.backend.dto.pairing.PairingQuestionsRequest;
import com.chococo.backend.dto.pairing.PairingQuestionsResponse;
import com.chococo.backend.dto.pairing.PairingSuggestionRequest;
import com.chococo.backend.dto.pairing.PairingSuggestionResponse;
import com.chococo.backend.dto.pairing.PairingUsageResponse;
import com.chococo.backend.security.AuthenticatedUser;
import com.chococo.backend.service.PairingService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

// api-spec.md 3.3/3.4/3.5節。認証必須（SecurityConfigのanyRequest().authenticated()）
@RestController
@RequestMapping("/api/pairings")
public class PairingController {

    private final PairingService pairingService;

    public PairingController(PairingService pairingService) {
        this.pairingService = pairingService;
    }

    @GetMapping("/usage")
    public PairingUsageResponse usage(@AuthenticationPrincipal AuthenticatedUser user) {
        return pairingService.getUsage(user.id());
    }

    @PostMapping("/questions")
    public PairingQuestionsResponse questions(
            @AuthenticationPrincipal AuthenticatedUser user, @Valid @RequestBody PairingQuestionsRequest request) {
        return pairingService.generateQuestions(user.id(), request.sweetName());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PairingSuggestionResponse suggest(
            @AuthenticationPrincipal AuthenticatedUser user, @Valid @RequestBody PairingSuggestionRequest request) {
        List<PairingSuggestionRequest.AnswerDto> answers =
                request.answers() == null ? List.of() : request.answers();
        return pairingService.suggest(user.id(), request.sweetName(), answers);
    }
}
