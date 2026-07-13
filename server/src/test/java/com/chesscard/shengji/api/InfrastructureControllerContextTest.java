package com.chesscard.shengji.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationContext;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@WebMvcTest(InfrastructureController.class)
class InfrastructureControllerContextTest {
    @Autowired
    private ApplicationContext applicationContext;

    @MockBean
    private JdbcTemplate jdbcTemplate;

    @MockBean
    private RedisConnectionFactory redisConnectionFactory;

    @Test
    void createsInfrastructureControllerInSpringContext() {
        assertThat(applicationContext.getBean(InfrastructureController.class)).isNotNull();
    }
}
