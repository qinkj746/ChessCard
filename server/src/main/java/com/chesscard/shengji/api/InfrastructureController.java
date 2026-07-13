package com.chesscard.shengji.api;

import com.chesscard.shengji.api.dto.HealthResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.time.Instant;

@RestController
@RequestMapping("/api/infrastructure")
public class InfrastructureController {
    private final JdbcTemplate jdbcTemplate;
    private final RedisConnectionFactory redisConnectionFactory;
    private final String version;
    private final Clock clock;

    @Autowired
    public InfrastructureController(
            JdbcTemplate jdbcTemplate,
            RedisConnectionFactory redisConnectionFactory,
            @Value("${app.version:0.0.1-SNAPSHOT}") String version
    ) {
        this(jdbcTemplate, redisConnectionFactory, version, Clock.systemUTC());
    }

    InfrastructureController(JdbcTemplate jdbcTemplate, RedisConnectionFactory redisConnectionFactory) {
        this(jdbcTemplate, redisConnectionFactory, "0.0.1-SNAPSHOT", Clock.systemUTC());
    }

    InfrastructureController(
            JdbcTemplate jdbcTemplate,
            RedisConnectionFactory redisConnectionFactory,
            String version,
            Clock clock
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.redisConnectionFactory = redisConnectionFactory;
        this.version = version;
        this.clock = clock;
    }

    @GetMapping("/health")
    public HealthResponse health() {
        String database = databaseHealth();
        String redis = redisHealth();
        return new HealthResponse(overallStatus(database, redis), database, redis, version, Instant.now(clock));
    }

    private String overallStatus(String database, String redis) {
        if (!"UP".equals(database)) {
            return "DOWN";
        }
        if (!"UP".equals(redis)) {
            return "DEGRADED";
        }
        return "UP";
    }

    private String databaseHealth() {
        try {
            Integer value = jdbcTemplate.queryForObject("select 1", Integer.class);
            return value != null && value == 1 ? "UP" : "DOWN";
        } catch (RuntimeException e) {
            return "DOWN";
        }
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