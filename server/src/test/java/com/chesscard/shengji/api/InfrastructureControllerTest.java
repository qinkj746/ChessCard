package com.chesscard.shengji.api;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.hamcrest.Matchers.blankOrNullString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class InfrastructureControllerTest {
    private final JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
    private final RedisConnectionFactory redisConnectionFactory = mock(RedisConnectionFactory.class);
    private final MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new InfrastructureController(jdbcTemplate, redisConnectionFactory))
            .build();

    @Test
    void reportsDegradedWhenRedisIsUnavailable() throws Exception {
        when(jdbcTemplate.queryForObject("select 1", Integer.class)).thenReturn(1);
        when(redisConnectionFactory.getConnection()).thenThrow(new RuntimeException("redis unavailable"));

        mockMvc.perform(get("/api/infrastructure/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("DEGRADED")))
                .andExpect(jsonPath("$.database", is("UP")))
                .andExpect(jsonPath("$.redis", is("DOWN")))
                .andExpect(jsonPath("$.version", not(blankOrNullString())))
                .andExpect(jsonPath("$.time", not(blankOrNullString())));
    }

    @Test
    void reportsDownWhenDatabaseIsUnavailable() throws Exception {
        RedisConnection redisConnection = mock(RedisConnection.class);
        when(jdbcTemplate.queryForObject("select 1", Integer.class)).thenThrow(new RuntimeException("database unavailable"));
        when(redisConnectionFactory.getConnection()).thenReturn(redisConnection);
        when(redisConnection.ping()).thenReturn("PONG");

        mockMvc.perform(get("/api/infrastructure/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("DOWN")))
                .andExpect(jsonPath("$.database", is("DOWN")))
                .andExpect(jsonPath("$.redis", is("UP")))
                .andExpect(jsonPath("$.version", not(blankOrNullString())))
                .andExpect(jsonPath("$.time", not(blankOrNullString())));
    }
}