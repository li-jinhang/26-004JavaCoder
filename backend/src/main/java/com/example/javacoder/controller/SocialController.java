package com.example.javacoder.controller;

import com.example.javacoder.model.ChatMessageRequest;
import com.example.javacoder.model.CurrentUser;
import com.example.javacoder.service.AdminAccountStore;
import com.example.javacoder.service.SocialStore;
import com.example.javacoder.service.UserStore;
import java.util.Map;
import java.util.Optional;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/social")
public class SocialController {

    private final SocialStore socialStore;
    private final UserStore userStore;
    private final AdminAccountStore adminAccountStore;

    public SocialController(SocialStore socialStore, UserStore userStore, AdminAccountStore adminAccountStore) {
        this.socialStore = socialStore;
        this.userStore = userStore;
        this.adminAccountStore = adminAccountStore;
    }

    @GetMapping("/users")
    public ResponseEntity<?> searchUsers(
            @RequestParam(defaultValue = "") String query,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization
    ) {
        return requirePrincipal(authorization)
                .<ResponseEntity<?>>map(user -> {
                    try {
                        return ResponseEntity.ok(socialStore.searchUsers(user, query));
                    } catch (IllegalArgumentException exception) {
                        return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
                    }
                })
                .orElseGet(this::unauthorized);
    }

    @GetMapping("/friends")
    public ResponseEntity<?> listFriends(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization
    ) {
        return requirePrincipal(authorization)
                .<ResponseEntity<?>>map(user -> {
                    try {
                        return ResponseEntity.ok(socialStore.listFriends(user));
                    } catch (IllegalArgumentException exception) {
                        return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
                    }
                })
                .orElseGet(this::unauthorized);
    }

    @PostMapping("/friends/{friendId}")
    public ResponseEntity<?> addFriend(
            @PathVariable long friendId,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization
    ) {
        return requirePrincipal(authorization)
                .<ResponseEntity<?>>map(user -> {
                    try {
                        return ResponseEntity.status(HttpStatus.CREATED).body(socialStore.addFriend(user, friendId));
                    } catch (IllegalArgumentException exception) {
                        return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
                    }
                })
                .orElseGet(this::unauthorized);
    }

    @GetMapping("/messages/{friendId}")
    public ResponseEntity<?> listMessages(
            @PathVariable long friendId,
            @RequestParam(defaultValue = "0") long afterId,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization
    ) {
        return requirePrincipal(authorization)
                .<ResponseEntity<?>>map(user -> {
                    try {
                        return ResponseEntity.ok(socialStore.listConversation(user, friendId, Math.max(0, afterId)));
                    } catch (IllegalArgumentException exception) {
                        return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
                    }
                })
                .orElseGet(this::unauthorized);
    }

    @PostMapping("/messages")
    public ResponseEntity<?> sendMessage(
            @RequestBody ChatMessageRequest request,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization
    ) {
        return requirePrincipal(authorization)
                .<ResponseEntity<?>>map(user -> {
                    try {
                        return ResponseEntity.status(HttpStatus.CREATED).body(socialStore.sendMessage(user, request));
                    } catch (IllegalArgumentException exception) {
                        return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
                    }
                })
                .orElseGet(this::unauthorized);
    }

    private Optional<CurrentUser> requirePrincipal(String authorization) {
        String token = extractBearerToken(authorization);
        return userStore.findByToken(token).or(() -> adminAccountStore.findByToken(token));
    }

    private ResponseEntity<?> unauthorized() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "请先登录。"));
    }

    private String extractBearerToken(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return null;
        }
        return authorization.substring("Bearer ".length()).trim();
    }
}
