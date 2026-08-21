package com.chococo.backend.controller;

import com.chococo.backend.dto.record.RecordListResponse;
import com.chococo.backend.dto.record.RecordResponse;
import com.chococo.backend.security.AuthenticatedUser;
import com.chococo.backend.service.RecordService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

// api-spec.md 3.5/3.6節。認証必須（SecurityConfigのanyRequest().authenticated()）
@RestController
@RequestMapping("/api/records")
@Validated
public class RecordController {

    private final RecordService recordService;

    public RecordController(RecordService recordService) {
        this.recordService = recordService;
    }

    @GetMapping
    public RecordListResponse list(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam int year,
            @RequestParam @Min(1) @Max(12) int month) {
        return recordService.listByMonth(user.id(), year, month);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public RecordResponse create(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam @NotBlank @Size(max = 100) String sweetName,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate recordDate,
            @RequestParam(required = false) @Size(max = 1000) String comment,
            @RequestParam(required = false) Long pairingSuggestionId,
            @RequestParam(required = false) MultipartFile photo) {
        return recordService.create(user.id(), sweetName, recordDate, comment, pairingSuggestionId, photo);
    }

    @GetMapping("/{id}")
    public RecordResponse detail(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id) {
        return recordService.getById(user.id(), id);
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public RecordResponse update(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long id,
            @RequestParam @NotBlank @Size(max = 100) String sweetName,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate recordDate,
            @RequestParam(required = false) @Size(max = 1000) String comment,
            @RequestParam(required = false, defaultValue = "false") boolean deletePhoto,
            @RequestParam(required = false) MultipartFile photo) {
        return recordService.update(user.id(), id, sweetName, recordDate, comment, deletePhoto, photo);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id) {
        recordService.delete(user.id(), id);
    }
}
