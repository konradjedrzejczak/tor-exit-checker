package com.example.torexit;

import static com.example.torexit.Fixtures.EXIT_1;
import static com.example.torexit.Fixtures.NOT_EXIT;
import static com.example.torexit.Fixtures.ip;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Tests the HTTP layer only; the registry is replaced by a mock. HEAD requests are checked for their status
 * only: leaving out the body is the web server's job, which MockMvc does not do. That part is covered by
 * TorExitCheckerEndToEndTest, which talks to a real server.
 */
@WebMvcTest(ExitNodeController.class)
class ExitNodeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ExitNodeRegistry registry;

    @BeforeEach
    void givenAListWithOneExitNode() {
        given(registry.isExitNode(ip(EXIT_1))).willReturn(true);
        given(registry.isExitNode(ip(NOT_EXIT))).willReturn(false);
        given(registry.lastUpdated()).willReturn(Instant.parse("2026-11-10T11:30:02Z"));
    }

    @Test
    void headIs200ForAnExitNode() throws Exception {
        mockMvc.perform(head("/" + EXIT_1))
                .andExpect(status().isOk());
    }

    @Test
    void headIs404ForOtherAddresses() throws Exception {
        mockMvc.perform(head("/" + NOT_EXIT))
                .andExpect(status().isNotFound());
    }

    @Test
    void getReturnsJsonForAnExitNode() throws Exception {
        mockMvc.perform(get("/" + EXIT_1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ip").value(EXIT_1))
                .andExpect(jsonPath("$.listUpdatedAt").value("2026-11-10T11:30:02Z"));
    }

    @Test
    void getIs404WithEmptyBodyForOtherAddresses() throws Exception {
        mockMvc.perform(get("/" + NOT_EXIT))
                .andExpect(status().isNotFound())
                .andExpect(content().string(""));
    }

    @ParameterizedTest
    @ValueSource(strings = {"999.1.1.1", "1.2.3", "01.2.3.4", "not-an-ip", "1.2.3.4.5"})
    void invalidAddressIs400(String text) throws Exception {
        mockMvc.perform(get("/" + text)).andExpect(status().isBadRequest());
        mockMvc.perform(head("/" + text)).andExpect(status().isBadRequest());
    }

    @Test
    void is503WhenTheListIsNotLoadedYet() throws Exception {
        // given
        given(registry.isExitNode(ip("192.0.2.3"))).willThrow(new ExitListUnavailableException());

        // when / then
        mockMvc.perform(get("/192.0.2.3")).andExpect(status().isServiceUnavailable());
        mockMvc.perform(head("/192.0.2.3")).andExpect(status().isServiceUnavailable());
    }
}
