package com.chesscard.shengji.persistence;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class GameSchemaInitializer implements ApplicationRunner {
    private final JdbcTemplate jdbcTemplate;

    public GameSchemaInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            jdbcTemplate.execute("alter table game_session add column room_id varchar(36)");
        } catch (Exception e) {
            // Column already exists, or the schema is managed by Hibernate in tests
        }
        try {
            jdbcTemplate.execute("alter table game_session modify snapshot_json longtext not null");
        } catch (Exception e) {
            // H2 or other non-MySQL databases don't support MODIFY syntax
            // In H2 with create-drop, the schema is already correct
        }
    }
}
