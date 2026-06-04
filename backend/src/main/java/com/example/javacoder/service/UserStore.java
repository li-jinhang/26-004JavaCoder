package com.example.javacoder.service;

import com.example.javacoder.model.CurrentUser;
import com.example.javacoder.model.UserRole;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class UserStore {

    private static final int SALT_BYTES = 16;
    private static final int HASH_BYTES = 32;
    private static final int PBKDF2_ITERATIONS = 120_000;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final JdbcTemplate jdbcTemplate;
    private final AtomicLong userId;

    public UserStore(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        initializeSchema();
        this.userId = new AtomicLong(Math.max(100, currentMaxUserId()));
    }

    public CurrentUser register(String username, String password) {
        String cleanUsername = normalizeVisibleUsername(username);
        validatePassword(password);

        String normalizedName = normalizeLookupName(cleanUsername);
        if (findStoredUser(normalizedName).isPresent()) {
            throw new IllegalArgumentException("用户名已存在。");
        }

        StoredPassword storedPassword = hashPassword(password);
        CurrentUser currentUser = new CurrentUser(userId.incrementAndGet(), cleanUsername, UserRole.USER, Instant.now());

        try {
            jdbcTemplate.update(
                    """
                    INSERT INTO users (id, username, normalized_username, password_salt, password_hash, role, created_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                    """,
                    currentUser.id(),
                    currentUser.username(),
                    normalizedName,
                    storedPassword.salt(),
                    storedPassword.hash(),
                    currentUser.role().name(),
                    currentUser.createdAt().toString()
            );
        } catch (DataAccessException exception) {
            if (isUsernameUniqueConstraint(exception)) {
                throw new IllegalArgumentException("用户名已存在。");
            }
            throw exception;
        }

        return currentUser;
    }

    public Optional<CurrentUser> login(String username, String password) {
        if (username == null || password == null) {
            return Optional.empty();
        }

        Optional<StoredUser> storedUser = findStoredUser(normalizeLookupName(username));
        if (storedUser.isEmpty() || !verifyPassword(password, storedUser.orElseThrow().password())) {
            return Optional.empty();
        }

        return Optional.of(storedUser.orElseThrow().user());
    }

    public String createSession(CurrentUser user) {
        String token = UUID.randomUUID() + "-" + UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO auth_sessions (token, user_id, created_at) VALUES (?, ?, ?)",
                token,
                user.id(),
                Instant.now().toString()
        );
        return token;
    }

    public Optional<CurrentUser> findByToken(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        List<CurrentUser> users = jdbcTemplate.query(
                """
                SELECT users.id, users.username, users.role, users.created_at
                FROM auth_sessions
                JOIN users ON users.id = auth_sessions.user_id
                WHERE auth_sessions.token = ?
                """,
                (resultSet, rowNumber) -> new CurrentUser(
                        resultSet.getLong("id"),
                        resultSet.getString("username"),
                        UserRole.valueOf(resultSet.getString("role")),
                        Instant.parse(resultSet.getString("created_at"))
                ),
                token
        );
        return users.stream().findFirst();
    }

    public Optional<CurrentUser> findById(long id) {
        List<CurrentUser> users = jdbcTemplate.query(
                """
                SELECT id, username, role, created_at
                FROM users
                WHERE id = ?
                """,
                (resultSet, rowNumber) -> new CurrentUser(
                        resultSet.getLong("id"),
                        resultSet.getString("username"),
                        UserRole.valueOf(resultSet.getString("role")),
                        Instant.parse(resultSet.getString("created_at"))
                ),
                id
        );
        return users.stream().findFirst();
    }

    public List<CurrentUser> searchUsers(String query) {
        String cleanQuery = query == null ? "" : query.trim();
        String normalizedQuery = "%" + cleanQuery.toLowerCase(Locale.ROOT) + "%";
        String idQuery = "%" + cleanQuery + "%";
        return jdbcTemplate.query(
                """
                SELECT id, username, role, created_at
                FROM users
                WHERE ? = ''
                   OR CAST(id AS TEXT) LIKE ?
                   OR normalized_username LIKE ?
                ORDER BY id ASC
                LIMIT 50
                """,
                (resultSet, rowNumber) -> new CurrentUser(
                        resultSet.getLong("id"),
                        resultSet.getString("username"),
                        UserRole.valueOf(resultSet.getString("role")),
                        Instant.parse(resultSet.getString("created_at"))
                ),
                cleanQuery,
                idQuery,
                normalizedQuery
        );
    }

    public boolean deleteUser(long id) {
        jdbcTemplate.update("DELETE FROM auth_sessions WHERE user_id = ?", id);
        return jdbcTemplate.update("DELETE FROM users WHERE id = ?", id) > 0;
    }

    public void logout(String token) {
        if (token != null) {
            jdbcTemplate.update("DELETE FROM auth_sessions WHERE token = ?", token);
        }
    }

    private void initializeSchema() {
        jdbcTemplate.execute(
                """
                CREATE TABLE IF NOT EXISTS users (
                    id INTEGER PRIMARY KEY,
                    username TEXT NOT NULL,
                    normalized_username TEXT NOT NULL UNIQUE,
                    password_salt TEXT NOT NULL,
                    password_hash TEXT NOT NULL,
                    role TEXT NOT NULL DEFAULT 'USER',
                    created_at TEXT NOT NULL
                )
                """
        );
        ensureRoleColumn();
        jdbcTemplate.execute(
                """
                CREATE TABLE IF NOT EXISTS auth_sessions (
                    token TEXT PRIMARY KEY,
                    user_id INTEGER NOT NULL,
                    created_at TEXT NOT NULL,
                    FOREIGN KEY (user_id) REFERENCES users(id)
                )
                """
        );
        ensureRegularUserRoles();
    }

    private void ensureRoleColumn() {
        boolean hasRoleColumn = jdbcTemplate.queryForList("PRAGMA table_info(users)").stream()
                .anyMatch(column -> "role".equals(column.get("name")));
        if (!hasRoleColumn) {
            jdbcTemplate.execute("ALTER TABLE users ADD COLUMN role TEXT NOT NULL DEFAULT 'USER'");
        }
    }

    private void ensureRegularUserRoles() {
        jdbcTemplate.update("UPDATE users SET role = 'USER' WHERE role <> 'USER'");
    }

    private long currentMaxUserId() {
        Long maxId = jdbcTemplate.queryForObject("SELECT COALESCE(MAX(id), 100) FROM users", Long.class);
        return maxId == null ? 100 : maxId;
    }

    private Optional<StoredUser> findStoredUser(String normalizedName) {
        List<StoredUser> users = jdbcTemplate.query(
                """
                SELECT id, username, password_salt, password_hash, role, created_at
                FROM users
                WHERE normalized_username = ?
                """,
                (resultSet, rowNumber) -> new StoredUser(
                        new CurrentUser(
                                resultSet.getLong("id"),
                                resultSet.getString("username"),
                                UserRole.valueOf(resultSet.getString("role")),
                                Instant.parse(resultSet.getString("created_at"))
                        ),
                        new StoredPassword(
                                resultSet.getString("password_salt"),
                                resultSet.getString("password_hash")
                        )
                ),
                normalizedName
        );
        return users.stream().findFirst();
    }

    private boolean isUsernameUniqueConstraint(DataAccessException exception) {
        String message = exception.getMessage();
        return message != null && message.contains("users.normalized_username");
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
