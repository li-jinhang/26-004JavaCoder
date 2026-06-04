package com.example.javacoder.service;

import com.example.javacoder.model.CurrentUser;
import com.example.javacoder.model.UserRole;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

@Service
public class AdminAccountStore {

    private final ObjectMapper objectMapper;
    private final Resource accountsResource;
    private final Map<String, CurrentUser> sessions = new ConcurrentHashMap<>();

    public AdminAccountStore(
            ObjectMapper objectMapper,
            @Value("${javacoder.admin.accounts:classpath:admin-users.json}") Resource accountsResource
    ) {
        this.objectMapper = objectMapper;
        this.accountsResource = accountsResource;
    }

    public Optional<CurrentUser> login(String username, String password) {
        if (username == null || password == null) {
            return Optional.empty();
        }

        List<AdminAccount> admins = readAccounts().admins();
        for (int index = 0; index < admins.size(); index++) {
            AdminAccount account = admins.get(index);
            if (account.matches(username, password)) {
                return Optional.of(new CurrentUser(-1L - index, account.username().trim(), UserRole.ADMIN, Instant.EPOCH));
            }
        }
        return Optional.empty();
    }

    public String createSession(CurrentUser admin) {
        if (admin.role() != UserRole.ADMIN) {
            throw new IllegalArgumentException("Only administrator accounts can create administrator sessions.");
        }

        String token = UUID.randomUUID() + "-" + UUID.randomUUID();
        sessions.put(token, admin);
        return token;
    }

    public Optional<CurrentUser> findByToken(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(sessions.get(token));
    }

    public void logout(String token) {
        if (token != null) {
            sessions.remove(token);
        }
    }

    private AdminAccounts readAccounts() {
        try (InputStream inputStream = accountsResource.getInputStream()) {
            AdminAccounts accounts = objectMapper.readValue(inputStream, AdminAccounts.class);
            return accounts == null || accounts.admins() == null ? new AdminAccounts(List.of()) : accounts;
        } catch (IOException exception) {
            throw new IllegalStateException("读取管理员账号 JSON 文件失败。", exception);
        }
    }

    private record AdminAccounts(List<AdminAccount> admins) {
    }

    private record AdminAccount(String username, String password) {
        boolean matches(String inputUsername, String inputPassword) {
            return username != null
                    && password != null
                    && username.trim().equalsIgnoreCase(inputUsername.trim())
                    && password.equals(inputPassword);
        }
    }
}
