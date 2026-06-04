package com.example.javacoder.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.javacoder.model.UserRole;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;

class AdminAccountStoreTest {

    @Test
    void authenticatesAdministratorsFromJsonAndManagesSessions() {
        AdminAccountStore store = new AdminAccountStore(
                new ObjectMapper(),
                new ByteArrayResource(
                        """
                        {
                          "admins": [
                            { "username": "root", "password": "secret123" }
                          ]
                        }
                        """.getBytes(StandardCharsets.UTF_8)
                )
        );

        assertThat(store.login("root", "bad-password")).isEmpty();

        var admin = store.login("ROOT", "secret123").orElseThrow();
        assertThat(admin.role()).isEqualTo(UserRole.ADMIN);
        assertThat(admin.username()).isEqualTo("root");

        String token = store.createSession(admin);
        assertThat(store.findByToken(token)).hasValue(admin);

        store.logout(token);
        assertThat(store.findByToken(token)).isEmpty();
    }
}
