package com.example.javacoder.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.javacoder.model.CurrentUser;
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
                });
        assertThat(secondStore.findByToken(token))
                .hasValueSatisfying(user -> assertThat(user.id()).isEqualTo(registered.id()));
        assertThatThrownBy(() -> secondStore.register("ALICE_01", "other-secret"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("用户名已存在。");
    }

    private JdbcTemplate sqliteJdbcTemplate() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.sqlite.JDBC");
        dataSource.setUrl("jdbc:sqlite:" + tempDir.resolve("javacoder-test.sqlite"));
        return new JdbcTemplate(dataSource);
    }
}
