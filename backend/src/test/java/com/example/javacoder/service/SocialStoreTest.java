package com.example.javacoder.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.javacoder.model.ChatMessageRequest;
import com.example.javacoder.model.CurrentUser;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class SocialStoreTest {

    @TempDir
    Path tempDir;

    @Test
    void addsFriendsSendsMessagesAndTracksUnreadCounts() {
        JdbcTemplate jdbcTemplate = sqliteJdbcTemplate();
        UserStore userStore = new UserStore(jdbcTemplate);
        SocialStore socialStore = new SocialStore(jdbcTemplate, userStore);
        CurrentUser alice = userStore.register("Alice_01", "secret123");
        CurrentUser bob = userStore.register("Bob_01", "secret123");

        socialStore.addFriend(alice, bob.id());
        assertThat(socialStore.searchUsers(alice, "bob"))
                .singleElement()
                .satisfies(user -> {
                    assertThat(user.id()).isEqualTo(bob.id());
                    assertThat(user.friend()).isTrue();
                    assertThat(user.currentUser()).isFalse();
                });

        assertThat(socialStore.sendMessage(alice, new ChatMessageRequest(
                bob.id(),
                "TEXT",
                "看一下这题的边界条件。",
                null,
                null,
                null
        )).id()).isGreaterThan(3000);

        assertThat(socialStore.listFriends(bob))
                .singleElement()
                .satisfies(friend -> {
                    assertThat(friend.id()).isEqualTo(alice.id());
                    assertThat(friend.unreadCount()).isEqualTo(1);
                    assertThat(friend.lastMessage().content()).isEqualTo("看一下这题的边界条件。");
                });

        assertThat(socialStore.listConversation(bob, alice.id(), 0))
                .singleElement()
                .satisfies(message -> assertThat(message.senderId()).isEqualTo(alice.id()));
        assertThat(socialStore.listFriends(bob))
                .singleElement()
                .satisfies(friend -> assertThat(friend.unreadCount()).isZero());
    }

    @Test
    void onlyAllowsMessagesBetweenFriends() {
        JdbcTemplate jdbcTemplate = sqliteJdbcTemplate();
        UserStore userStore = new UserStore(jdbcTemplate);
        SocialStore socialStore = new SocialStore(jdbcTemplate, userStore);
        CurrentUser alice = userStore.register("Alice_01", "secret123");
        CurrentUser bob = userStore.register("Bob_01", "secret123");

        assertThatThrownBy(() -> socialStore.sendMessage(alice, new ChatMessageRequest(
                bob.id(),
                "TEXT",
                "hello",
                null,
                null,
                null
        )))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("只能给好友发送消息。");
    }

    private JdbcTemplate sqliteJdbcTemplate() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.sqlite.JDBC");
        dataSource.setUrl("jdbc:sqlite:" + tempDir.resolve("javacoder-social-test.sqlite"));
        return new JdbcTemplate(dataSource);
    }
}
