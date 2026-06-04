package com.example.javacoder.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.javacoder.model.CurrentUser;
import com.example.javacoder.model.UserRole;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class UserStoreTest {

    @TempDir
    Path tempDir;

    @Test
    void persistsUsersPasswordHashesAndSessionsAcrossStoreInstances() {
        UserStore firstStore = new UserStore(sqliteJdbcTemplate());
        CurrentUser registered = firstStore.register("Alice_01", "secret123");
        String token = firstStore.createSession(registered);

        UserStore secondStore = new UserStore(sqliteJdbcTemplate());

        assertThat(secondStore.login("alice_01", "secret123"))
                .hasValueSatisfying(user -> {
                    assertThat(user.id()).isEqualTo(registered.id());
                    assertThat(user.username()).isEqualTo("Alice_01");
                    assertThat(user.role()).isEqualTo(UserRole.USER);
                });
        assertThat(secondStore.findByToken(token))
                .hasValueSatisfying(user -> assertThat(user.id()).isEqualTo(registered.id()));
        assertThatThrownBy(() -> secondStore.register("ALICE_01", "other-secret"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("用户名已存在。");
    }

    @Test
    void assignsUserRoleSearchesUsersAndDeletesAccounts() {
        UserStore store = new UserStore(sqliteJdbcTemplate());
        CurrentUser alice = store.register("Admin_01", "secret123");
        CurrentUser user = store.register("Bob_01", "secret123");
        String userToken = store.createSession(user);

        assertThat(alice.role()).isEqualTo(UserRole.USER);
        assertThat(user.role()).isEqualTo(UserRole.USER);
        assertThat(store.searchUsers("bob"))
                .extracting(CurrentUser::id)
                .containsExactly(user.id());
        assertThat(store.searchUsers(String.valueOf(alice.id())))
                .extracting(CurrentUser::username)
                .contains("Admin_01");

        assertThat(store.deleteUser(user.id())).isTrue();
        assertThat(store.findById(user.id())).isEmpty();
        assertThat(store.findByToken(userToken)).isEmpty();
    }

    private JdbcTemplate sqliteJdbcTemplate() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.sqlite.JDBC");
        dataSource.setUrl("jdbc:sqlite:" + tempDir.resolve("javacoder-test.sqlite"));
        return new JdbcTemplate(dataSource);
    }
}
