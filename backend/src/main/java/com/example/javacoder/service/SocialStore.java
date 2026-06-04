package com.example.javacoder.service;

import com.example.javacoder.model.ChatMessage;
import com.example.javacoder.model.ChatMessageRequest;
import com.example.javacoder.model.CurrentUser;
import com.example.javacoder.model.FriendView;
import com.example.javacoder.model.SocialUserView;
import com.example.javacoder.model.UserRole;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class SocialStore {

    private static final int MAX_MESSAGE_LENGTH = 2_000;
    private static final int MAX_CODE_LENGTH = 65_536;
    private static final List<String> MESSAGE_TYPES = List.of("TEXT", "PROBLEM", "CODE");

    private final JdbcTemplate jdbcTemplate;
    private final UserStore userStore;
    private final AtomicLong messageId;

    public SocialStore(JdbcTemplate jdbcTemplate, UserStore userStore) {
        this.jdbcTemplate = jdbcTemplate;
        this.userStore = userStore;
        initializeSchema();
        this.messageId = new AtomicLong(Math.max(3000, currentMaxMessageId()));
    }

    public List<SocialUserView> searchUsers(CurrentUser currentUser, String query) {
        requireRegularUser(currentUser);
        return userStore.searchUsers(query).stream()
                .map(user -> SocialUserView.from(user, areFriends(currentUser.id(), user.id()), currentUser.id()))
                .toList();
    }

    public FriendView addFriend(CurrentUser currentUser, long friendId) {
        requireRegularUser(currentUser);
        if (currentUser.id() == friendId) {
            throw new IllegalArgumentException("不能添加自己为好友。");
        }

        CurrentUser friend = userStore.findById(friendId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在。"));
        requireRegularUser(friend);

        UserPair pair = UserPair.of(currentUser.id(), friend.id());
        jdbcTemplate.update(
                """
                INSERT OR IGNORE INTO friendships (user_one_id, user_two_id, created_at)
                VALUES (?, ?, ?)
                """,
                pair.userOneId(),
                pair.userTwoId(),
                Instant.now().toString()
        );
        return friendView(currentUser.id(), friend);
    }

    public List<FriendView> listFriends(CurrentUser currentUser) {
        requireRegularUser(currentUser);
        return jdbcTemplate.query(
                """
                SELECT users.id, users.username, users.role, users.created_at, friendships.created_at AS friend_since
                FROM friendships
                JOIN users ON users.id = CASE
                    WHEN friendships.user_one_id = ? THEN friendships.user_two_id
                    ELSE friendships.user_one_id
                END
                WHERE friendships.user_one_id = ? OR friendships.user_two_id = ?
                ORDER BY COALESCE((
                    SELECT MAX(created_at)
                    FROM chat_messages
                    WHERE (sender_id = ? AND recipient_id = users.id)
                       OR (sender_id = users.id AND recipient_id = ?)
                ), friendships.created_at) DESC
                """,
                (resultSet, rowNumber) -> friendViewFromRow(currentUser.id(), resultSet),
                currentUser.id(),
                currentUser.id(),
                currentUser.id(),
                currentUser.id(),
                currentUser.id()
        );
    }

    public ChatMessage sendMessage(CurrentUser sender, ChatMessageRequest request) {
        requireRegularUser(sender);
        NormalizedMessageInput input = normalizeMessage(request);
        CurrentUser recipient = userStore.findById(input.recipientId())
                .orElseThrow(() -> new IllegalArgumentException("收件人不存在。"));
        requireRegularUser(recipient);
        if (recipient.id() == sender.id()) {
            throw new IllegalArgumentException("不能给自己发送消息。");
        }
        if (!areFriends(sender.id(), recipient.id())) {
            throw new IllegalArgumentException("只能给好友发送消息。");
        }

        ChatMessage message = new ChatMessage(
                messageId.incrementAndGet(),
                sender.id(),
                sender.username(),
                recipient.id(),
                recipient.username(),
                input.type(),
                input.content(),
                input.problemId(),
                input.problemTitle(),
                input.code(),
                Instant.now(),
                null
        );
        insertMessage(message);
        return message;
    }

    public List<ChatMessage> listConversation(CurrentUser currentUser, long friendId, long afterId) {
        requireRegularUser(currentUser);
        CurrentUser friend = userStore.findById(friendId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在。"));
        if (!areFriends(currentUser.id(), friend.id())) {
            throw new IllegalArgumentException("只能查看好友消息。");
        }

        markRead(currentUser.id(), friend.id());
        return jdbcTemplate.query(
                """
                SELECT messages.id, messages.sender_id, sender.username AS sender_name,
                       messages.recipient_id, recipient.username AS recipient_name,
                       messages.type, messages.content, messages.problem_id, messages.problem_title,
                       messages.code, messages.created_at, messages.read_at
                FROM chat_messages messages
                JOIN users sender ON sender.id = messages.sender_id
                JOIN users recipient ON recipient.id = messages.recipient_id
                WHERE messages.id > ?
                  AND ((messages.sender_id = ? AND messages.recipient_id = ?)
                    OR (messages.sender_id = ? AND messages.recipient_id = ?))
                ORDER BY messages.id ASC
                LIMIT 200
                """,
                this::mapMessage,
                afterId,
                currentUser.id(),
                friend.id(),
                friend.id(),
                currentUser.id()
        );
    }

    public boolean areFriends(long firstUserId, long secondUserId) {
        if (firstUserId == secondUserId) {
            return false;
        }
        UserPair pair = UserPair.of(firstUserId, secondUserId);
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM friendships WHERE user_one_id = ? AND user_two_id = ?",
                Integer.class,
                pair.userOneId(),
                pair.userTwoId()
        );
        return count != null && count > 0;
    }

    private FriendView friendView(long currentUserId, CurrentUser friend) {
        String friendSince = jdbcTemplate.queryForObject(
                "SELECT created_at FROM friendships WHERE user_one_id = ? AND user_two_id = ?",
                String.class,
                UserPair.of(currentUserId, friend.id()).userOneId(),
                UserPair.of(currentUserId, friend.id()).userTwoId()
        );
        return new FriendView(
                friend.id(),
                friend.username(),
                friend.createdAt(),
                Instant.parse(friendSince),
                latestMessage(currentUserId, friend.id()).orElse(null),
                unreadCount(currentUserId, friend.id())
        );
    }

    private FriendView friendViewFromRow(long currentUserId, ResultSet resultSet) throws SQLException {
        CurrentUser friend = new CurrentUser(
                resultSet.getLong("id"),
                resultSet.getString("username"),
                UserRole.valueOf(resultSet.getString("role")),
                Instant.parse(resultSet.getString("created_at"))
        );
        return new FriendView(
                friend.id(),
                friend.username(),
                friend.createdAt(),
                Instant.parse(resultSet.getString("friend_since")),
                latestMessage(currentUserId, friend.id()).orElse(null),
                unreadCount(currentUserId, friend.id())
        );
    }

    private Optional<ChatMessage> latestMessage(long currentUserId, long friendId) {
        List<ChatMessage> messages = jdbcTemplate.query(
                """
                SELECT messages.id, messages.sender_id, sender.username AS sender_name,
                       messages.recipient_id, recipient.username AS recipient_name,
                       messages.type, messages.content, messages.problem_id, messages.problem_title,
                       messages.code, messages.created_at, messages.read_at
                FROM chat_messages messages
                JOIN users sender ON sender.id = messages.sender_id
                JOIN users recipient ON recipient.id = messages.recipient_id
                WHERE (messages.sender_id = ? AND messages.recipient_id = ?)
                   OR (messages.sender_id = ? AND messages.recipient_id = ?)
                ORDER BY messages.id DESC
                LIMIT 1
                """,
                this::mapMessage,
                currentUserId,
                friendId,
                friendId,
                currentUserId
        );
        return messages.stream().findFirst();
    }

    private int unreadCount(long currentUserId, long friendId) {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM chat_messages
                WHERE sender_id = ? AND recipient_id = ? AND read_at IS NULL
                """,
                Integer.class,
                friendId,
                currentUserId
        );
        return count == null ? 0 : count;
    }

    private void markRead(long currentUserId, long friendId) {
        jdbcTemplate.update(
                """
                UPDATE chat_messages
                SET read_at = ?
                WHERE sender_id = ? AND recipient_id = ? AND read_at IS NULL
                """,
                Instant.now().toString(),
                friendId,
                currentUserId
        );
    }

    private void insertMessage(ChatMessage message) {
        jdbcTemplate.update(
                """
                INSERT INTO chat_messages (
                    id, sender_id, recipient_id, type, content, problem_id, problem_title,
                    code, created_at, read_at
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                message.id(),
                message.senderId(),
                message.recipientId(),
                message.type(),
                message.content(),
                message.problemId(),
                message.problemTitle(),
                message.code(),
                message.createdAt().toString(),
                null
        );
    }

    private ChatMessage mapMessage(ResultSet resultSet, int rowNumber) throws SQLException {
        long problemId = resultSet.getLong("problem_id");
        boolean problemIdMissing = resultSet.wasNull();
        String readAt = resultSet.getString("read_at");
        return new ChatMessage(
                resultSet.getLong("id"),
                resultSet.getLong("sender_id"),
                resultSet.getString("sender_name"),
                resultSet.getLong("recipient_id"),
                resultSet.getString("recipient_name"),
                resultSet.getString("type"),
                resultSet.getString("content"),
                problemIdMissing ? null : problemId,
                resultSet.getString("problem_title"),
                resultSet.getString("code"),
                Instant.parse(resultSet.getString("created_at")),
                readAt == null ? null : Instant.parse(readAt)
        );
    }

    private NormalizedMessageInput normalizeMessage(ChatMessageRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("请输入消息内容。");
        }

        String type = request.type() == null || request.type().isBlank()
                ? "TEXT"
                : request.type().trim().toUpperCase(Locale.ROOT);
        if (!MESSAGE_TYPES.contains(type)) {
            throw new IllegalArgumentException("消息类型不支持。");
        }

        String content = request.content() == null ? "" : request.content().trim();
        String code = request.code() == null ? "" : request.code().trim();
        String problemTitle = request.problemTitle() == null ? "" : request.problemTitle().trim();

        if ("TEXT".equals(type) && content.isBlank()) {
            throw new IllegalArgumentException("请输入消息内容。");
        }
        if ("PROBLEM".equals(type) && (request.problemId() == null || problemTitle.isBlank())) {
            throw new IllegalArgumentException("请选择要分享的题目。");
        }
        if ("CODE".equals(type) && code.isBlank()) {
            throw new IllegalArgumentException("请输入要分享的代码。");
        }
        if (content.length() > MAX_MESSAGE_LENGTH) {
            throw new IllegalArgumentException("消息内容不能超过 2000 个字符。");
        }
        if (code.length() > MAX_CODE_LENGTH) {
            throw new IllegalArgumentException("代码片段不能超过 65536 个字符。");
        }
        if (problemTitle.length() > 120) {
            throw new IllegalArgumentException("题目标题不能超过 120 个字符。");
        }

        return new NormalizedMessageInput(
                request.recipientId(),
                type,
                content,
                request.problemId(),
                problemTitle,
                code
        );
    }

    private void initializeSchema() {
        jdbcTemplate.execute(
                """
                CREATE TABLE IF NOT EXISTS friendships (
                    user_one_id INTEGER NOT NULL,
                    user_two_id INTEGER NOT NULL,
                    created_at TEXT NOT NULL,
                    PRIMARY KEY (user_one_id, user_two_id),
                    FOREIGN KEY (user_one_id) REFERENCES users(id),
                    FOREIGN KEY (user_two_id) REFERENCES users(id)
                )
                """
        );
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_friendships_user_two_id ON friendships(user_two_id)");
        jdbcTemplate.execute(
                """
                CREATE TABLE IF NOT EXISTS chat_messages (
                    id INTEGER PRIMARY KEY,
                    sender_id INTEGER NOT NULL,
                    recipient_id INTEGER NOT NULL,
                    type TEXT NOT NULL,
                    content TEXT NOT NULL,
                    problem_id INTEGER,
                    problem_title TEXT,
                    code TEXT,
                    created_at TEXT NOT NULL,
                    read_at TEXT,
                    FOREIGN KEY (sender_id) REFERENCES users(id),
                    FOREIGN KEY (recipient_id) REFERENCES users(id)
                )
                """
        );
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_chat_messages_pair ON chat_messages(sender_id, recipient_id, id)");
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_chat_messages_recipient_read ON chat_messages(recipient_id, read_at)");
    }

    private long currentMaxMessageId() {
        Long maxId = jdbcTemplate.queryForObject("SELECT COALESCE(MAX(id), 3000) FROM chat_messages", Long.class);
        return maxId == null ? 3000 : maxId;
    }

    private void requireRegularUser(CurrentUser user) {
        if (user == null || user.role() != UserRole.USER || user.id() <= 0) {
            throw new IllegalArgumentException("普通用户才可以使用好友消息功能。");
        }
    }

    private record UserPair(long userOneId, long userTwoId) {
        static UserPair of(long firstUserId, long secondUserId) {
            return firstUserId < secondUserId
                    ? new UserPair(firstUserId, secondUserId)
                    : new UserPair(secondUserId, firstUserId);
        }
    }

    private record NormalizedMessageInput(
            long recipientId,
            String type,
            String content,
            Long problemId,
            String problemTitle,
            String code
    ) {
    }
}
