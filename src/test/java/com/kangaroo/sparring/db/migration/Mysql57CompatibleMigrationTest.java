package com.kangaroo.sparring.db.migration;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class Mysql57CompatibleMigrationTest {

    @Test
    void v5AndV6_shouldNotUseIfExistsSyntaxThatBreaksOnMysql57() throws IOException {
        String v5 = readMigration("src/main/resources/db/migration/V5__refactor_food_columns.sql");
        String v6 = readMigration("src/main/resources/db/migration/V6__refactor_recommend_food_columns.sql");

        assertThat(v5.toLowerCase())
                .doesNotContain("drop column if exists")
                .doesNotContain("add column if not exists");
        assertThat(v6.toLowerCase())
                .doesNotContain("drop column if exists")
                .doesNotContain("add column if not exists");
    }

    private String readMigration(String filePath) throws IOException {
        return Files.readString(Path.of(filePath), StandardCharsets.UTF_8);
    }
}
