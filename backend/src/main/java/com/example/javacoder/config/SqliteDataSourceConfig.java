package com.example.javacoder.config;

import java.nio.file.Files;
import java.nio.file.Path;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

@Configuration
public class SqliteDataSourceConfig {

    @Bean
    public DataSource dataSource(@Value("${javacoder.storage.sqlite-path}") String sqlitePath) {
        Path databasePath = Path.of(sqlitePath).toAbsolutePath().normalize();
        Path parent = databasePath.getParent();
        if (parent != null) {
            try {
                Files.createDirectories(parent);
            } catch (Exception exception) {
                throw new IllegalStateException("Failed to create SQLite data directory: " + parent, exception);
            }
        }

        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.sqlite.JDBC");
        dataSource.setUrl("jdbc:sqlite:" + databasePath);
        return dataSource;
    }
}
