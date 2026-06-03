package com.example.javacoder.service;

import com.example.javacoder.model.CurrentUser;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.time.Instant;
import java.util.Base64;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import org.springframework.stereotype.Service;

@Service
public class UserStore {

    private static final int SALT_BYTES = 16;
    private static final int HASH_BYTES = 32;
    private static final int PBKDF2_ITERATIONS = 120_000;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final AtomicLong userId = new AtomicLong(100);
    private final ConcurrentMap<String, StoredUser> usersByNormalizedName = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, CurrentUser> sessionsByToken = new ConcurrentHashMap<>();

    public CurrentUser register(String username, String password) {
        String cleanUsername = normalizeVisibleUsername(username);
        validatePassword(password);

        String normalizedName = normalizeLookupName(cleanUsername);
        StoredPassword storedPassword = hashPassword(password);
        CurrentUser currentUser = new CurrentUser(userId.incrementAndGet(), cleanUsername, Instant.now());
        StoredUser storedUser = new StoredUser(currentUser, storedPassword);

        StoredUser existing = usersByNormalizedName.putIfAbsent(normalizedName, storedUser);
        if (existing != null) {
            throw new IllegalArgumentException("用户名已存在。");
        }

        return currentUser;
    }

    public Optional<CurrentUser> login(String username, String password) {
        if (username == null || password == null) {
            return Optional.empty();
        }

        StoredUser storedUser = usersByNormalizedName.get(normalizeLookupName(username));
        if (storedUser == null || !verifyPassword(password, storedUser.password())) {
            return Optional.empty();
        }

        return Optional.of(storedUser.user());
    }

    public String createSession(CurrentUser user) {
        String token = UUID.randomUUID() + "-" + UUID.randomUUID();
        sessionsByToken.put(token, user);
        return token;
    }

    public Optional<CurrentUser> findByToken(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(sessionsByToken.get(token));
    }

    public void logout(String token) {
        if (token != null) {
            sessionsByToken.remove(token);
        }
    }

    private String normalizeVisibleUsername(String username) {
        if (username == null) {
            throw new IllegalArgumentException("请输入用户名。");
        }

        String cleanUsername = username.trim();
        if (cleanUsername.length() < 3 || cleanUsername.length() > 20) {
            throw new IllegalArgumentException("用户名长度需为 3-20 个字符。");
        }
        if (!cleanUsername.matches("[A-Za-z0-9_\\u4e00-\\u9fa5]+")) {
            throw new IllegalArgumentException("用户名只能包含中文、字母、数字或下划线。");
        }

        return cleanUsername;
    }

    private String normalizeLookupName(String username) {
        return username.trim().toLowerCase(Locale.ROOT);
    }

    private void validatePassword(String password) {
        if (password == null || password.length() < 6 || password.length() > 64) {
            throw new IllegalArgumentException("密码长度需为 6-64 个字符。");
        }
        if (password.isBlank()) {
            throw new IllegalArgumentException("密码不能全为空白字符。");
        }
    }

    private StoredPassword hashPassword(String password) {
        byte[] salt = new byte[SALT_BYTES];
        SECURE_RANDOM.nextBytes(salt);
        byte[] hash = pbkdf2(password, salt);
        return new StoredPassword(
                Base64.getEncoder().encodeToString(salt),
                Base64.getEncoder().encodeToString(hash)
        );
    }

    private boolean verifyPassword(String password, StoredPassword storedPassword) {
        byte[] salt = Base64.getDecoder().decode(storedPassword.salt());
        byte[] expectedHash = Base64.getDecoder().decode(storedPassword.hash());
        byte[] actualHash = pbkdf2(password, salt);

        if (actualHash.length != expectedHash.length) {
            return false;
        }

        int diff = 0;
        for (int i = 0; i < actualHash.length; i++) {
            diff |= actualHash[i] ^ expectedHash[i];
        }
        return diff == 0;
    }

    private byte[] pbkdf2(String password, byte[] salt) {
        try {
            PBEKeySpec spec = new PBEKeySpec(
                    password.toCharArray(),
                    salt,
                    PBKDF2_ITERATIONS,
                    HASH_BYTES * Byte.SIZE
            );
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            return factory.generateSecret(spec).getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException exception) {
            throw new IllegalStateException("密码哈希失败。", exception);
        }
    }

    private record StoredUser(CurrentUser user, StoredPassword password) {
    }

    private record StoredPassword(String salt, String hash) {
    }
}
