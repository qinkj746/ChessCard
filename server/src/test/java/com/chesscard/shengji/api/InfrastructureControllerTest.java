package com.chesscard.shengji.api;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Tag("integration")
class InfrastructureControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void reportsMysqlAndRedisHealth() throws Exception {
        mockMvc.perform(get("/api/infrastructure/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mysql", is("UP")))
                .andExpect(jsonPath("$.redis", anyOf(is("UP"), is("DOWN"))));
    }
}
