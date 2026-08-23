package com.weddinggames.backend;

import static org.assertj.core.api.Assertions.assertThat;

import com.weddinggames.backend.support.AbstractIntegrationTest;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Boots the real Spring context against a Testcontainers PostgreSQL instance so that Flyway
 * migrations run for real (not H2) and Hibernate's ddl-auto=validate confirms every entity
 * mapping matches the migrated schema exactly.
 */
class FlywayMigrationIT extends AbstractIntegrationTest {

    @Autowired
    private DataSource dataSource;

    @Test
    void allMigrationsAppliedSuccessfully() {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        List<String> failedMigrations = jdbcTemplate.queryForList(
                "SELECT version FROM flyway_schema_history WHERE success = false", String.class);

        assertThat(failedMigrations).isEmpty();

        Integer appliedCount =
                jdbcTemplate.queryForObject("SELECT count(*) FROM flyway_schema_history WHERE success = true", Integer.class);
        assertThat(appliedCount).isGreaterThanOrEqualTo(17);
    }

    @Test
    void coreTablesExistWithExpectedConstraints() {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        List<String> tables = jdbcTemplate.queryForList(
                "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'", String.class);

        assertThat(tables)
                .contains(
                        "wedding_event",
                        "participant",
                        "invitation",
                        "staff_account",
                        "app_session",
                        "pairing_exclusion",
                        "lobby",
                        "lobby_participant",
                        "game_character",
                        "team",
                        "team_member",
                        "game",
                        "question",
                        "answer",
                        "vote",
                        "score");

        Integer uniquePairConstraintCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM pg_constraint WHERE conname = 'uq_pairing_exclusion_pair'", Integer.class);
        assertThat(uniquePairConstraintCount).isEqualTo(1);
    }
}
