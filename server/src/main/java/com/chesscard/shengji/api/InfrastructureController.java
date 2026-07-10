package com.chesscard.shengji.api;

import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/infrastructure")
public class InfrastructureController {
    private final JdbcTemplate jdbcTemplate;
    private final RedisConnectionFactory redisConnectionFactory;

    public InfrastructureController(JdbcTemplate jdbcTemplate, RedisConnectionFactory redisConnectionFactory) {
        this.jdbcTemplate = jdbcTemplate;
        this.redisConnectionFactory = redisConnectionFactory;
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of(
                "mysql", mysqlHealth(),
                "redis", redisHealth()
        );
    }

    private String mysqlHealth() {
        Integer value = jdbcTemplate.queryForObject("select 1", Integer.class);
        return value != null && value == 1 ? "UP" : "DOWN";
    }

    private String redisHealth() {
        try (RedisConnection connection = redisConnectionFactory.getConnection()) {
            String pong = connection.ping();
            return "PONG".equalsIgnoreCase(pong) ? "UP" : "DOWN";
        } catch (RuntimeException e) {
            return "DOWN";
        }
    }
}
