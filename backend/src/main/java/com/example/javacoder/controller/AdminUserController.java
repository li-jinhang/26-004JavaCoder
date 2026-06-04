package com.example.javacoder.controller;

import com.example.javacoder.model.AdminUserView;
import com.example.javacoder.model.CurrentUser;
import com.example.javacoder.service.AdminAccountStore;
import com.example.javacoder.service.UserStore;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final UserStore userStore;
    private final AdminAccountStore adminAccountStore;

    public AdminUserController(UserStore userStore, AdminAccountStore adminAccountStore) {
        this.userStore = userStore;
        this.adminAccountStore = adminAccountStore;
    }

    @GetMapping
    public ResponseEntity<?> searchUsers(
            @RequestParam(value = "query", defaultValue = "") String query,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization
    ) {
        return requireAdmin(authorization)
                .<ResponseEntity<?>>map(admin -> {
                    List<AdminUserView> users = userStore.searchUsers(query).stream()
                            .map(user -> AdminUserView.from(user, admin.id()))
                            .toList();
                    return ResponseEntity.ok(users);
                })
                .orElseGet(this::forbiddenResponse);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(
            @PathVariable long id,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization
    ) {
        return requireAdmin(authorization)
                .<ResponseEntity<?>>map(admin -> {
                    if (admin.id() == id) {
                        return ResponseEntity.badRequest().body(Map.of("message", "不能删除当前登录的管理员账号。"));
                    }
                    if (userStore.findById(id).isEmpty()) {
                        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "用户不存在。"));
                    }
                    userStore.deleteUser(id);
                    return ResponseEntity.noContent().build();
                })
                .orElseGet(this::forbiddenResponse);
    }

    private java.util.Optional<CurrentUser> requireAdmin(String authorization) {
        return adminAccountStore.findByToken(extractBearerToken(authorization));
    }

    private ResponseEntity<?> forbiddenResponse() {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "仅管理员可以访问用户管理。"));
    }

    private String extractBearerToken(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return null;
        }
        return authorization.substring("Bearer ".length()).trim();
    }
}
