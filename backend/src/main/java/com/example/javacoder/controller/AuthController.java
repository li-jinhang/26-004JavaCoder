package com.example.javacoder.controller;

import com.example.javacoder.model.AuthRequest;
import com.example.javacoder.model.AuthResponse;
import com.example.javacoder.model.CurrentUser;
import com.example.javacoder.service.AdminAccountStore;
import com.example.javacoder.service.UserStore;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserStore userStore;
    private final AdminAccountStore adminAccountStore;

    public AuthController(UserStore userStore, AdminAccountStore adminAccountStore) {
        this.userStore = userStore;
        this.adminAccountStore = adminAccountStore;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody AuthRequest request) {
        try {
            CurrentUser user = userStore.register(request.username(), request.password());
            String token = userStore.createSession(user);
            return ResponseEntity.status(HttpStatus.CREATED).body(new AuthResponse(token, user));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest request) {
        return adminAccountStore.login(request.username(), request.password())
                .<ResponseEntity<?>>map(admin -> ResponseEntity.ok(new AuthResponse(adminAccountStore.createSession(admin), admin)))
                .or(() -> userStore.login(request.username(), request.password())
                        .map(user -> ResponseEntity.ok(new AuthResponse(userStore.createSession(user), user))))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "用户名或密码错误。")));
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        String token = extractBearerToken(authorization);
        return adminAccountStore.findByToken(token)
                .or(() -> userStore.findByToken(token))
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "请先登录。")));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        String token = extractBearerToken(authorization);
        adminAccountStore.logout(token);
        userStore.logout(token);
        return ResponseEntity.noContent().build();
    }

    private String extractBearerToken(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return null;
        }
        return authorization.substring("Bearer ".length()).trim();
    }
}
