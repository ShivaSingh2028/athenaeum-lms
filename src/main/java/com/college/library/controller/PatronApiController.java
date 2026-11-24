package com.college.library.controller;

import com.college.library.entity.User;
import com.college.library.privacy.PatronDataExportDto;
import com.college.library.privacy.PrivacyService;
import com.college.library.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/patrons")
public class PatronApiController {

    private final PrivacyService privacyService;
    private final UserService userService;

    public PatronApiController(PrivacyService privacyService, UserService userService) {
        this.privacyService = privacyService;
        this.userService = userService;
    }

    @GetMapping("/me/data-export")
    public ResponseEntity<PatronDataExportDto> exportMyData(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.findByEmail(userDetails.getUsername()).orElseThrow();
        return ResponseEntity.ok(privacyService.exportPatronPersonalData(user.getId(), user));
    }

    @PostMapping("/me/anonymize")
    public ResponseEntity<?> anonymizeAccount(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.findByEmail(userDetails.getUsername()).orElseThrow();
        privacyService.anonymizePatronAccount(user.getId(), user);
        return ResponseEntity.ok(Map.of("status", "ANONYMIZED", "message", "Personal data scrubbed successfully."));
    }

    @PutMapping("/me/reading-history-privacy")
    public ResponseEntity<?> setPrivacy(@RequestParam boolean isPrivate,
                                        @AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.findByEmail(userDetails.getUsername()).orElseThrow();
        privacyService.setReadingHistoryPrivacy(user.getId(), isPrivate, user);
        return ResponseEntity.ok(Map.of("readingHistoryPrivate", isPrivate));
    }
}
