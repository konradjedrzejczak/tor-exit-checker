package com.example.torexit;

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

/** Tests the HTTP layer only; the service is replaced by a mock. */
@WebMvcTest(ExitNodeController.class)
class ExitNodeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ExitNodeService exitNodeService;

    @BeforeEach
    void setUp() {
        given(exitNodeService.isExitNode("192.0.2.1")).willReturn(true);
        given(exitNodeService.isExitNode("192.0.2.2")).willReturn(false);
        given(exitNodeService.lastUpdated()).willReturn(Instant.parse("2026-11-10T11:30:02Z"));
    }

    @Test
    void headIs200WithEmptyBodyForAnExitNode() throws Exception {
        mockMvc.perform(head("/192.0.2.1"))
                .andExpect(status().isOk())
                .andExpect(content().string(""));
    }

    @Test
    void headIs404WithEmptyBodyForOtherAddresses() throws Exception {
        mockMvc.perform(head("/192.0.2.2"))
                .andExpect(status().isNotFound())
                .andExpect(content().string(""));
    }

    @Test
    void getReturnsJsonForAnExitNode() throws Exception {
        mockMvc.perform(get("/192.0.2.1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ip").value("192.0.2.1"))
                .andExpect(jsonPath("$.listUpdatedAt").value("2026-11-10T11:30:02Z"));
    }

    @Test
    void getIs404WithEmptyBodyForOtherAddresses() throws Exception {
        mockMvc.perform(get("/192.0.2.2"))
                .andExpect(status().isNotFound())
                .andExpect(content().string(""));
    }

    @ParameterizedTest
    @ValueSource(strings = {"999.1.1.1", "1.2.3", "01.2.3.4", "not-an-ip", "1.2.3.4.5"})
    void invalidAddressIs400(String ip) throws Exception {
        mockMvc.perform(get("/" + ip)).andExpect(status().isBadRequest());
        mockMvc.perform(head("/" + ip)).andExpect(status().isBadRequest());
    }

    @Test
    void is503WhenTheListIsNotLoadedYet() throws Exception {
        given(exitNodeService.isExitNode("192.0.2.3")).willThrow(new ExitListUnavailableException());

        mockMvc.perform(get("/192.0.2.3")).andExpect(status().isServiceUnavailable());
        mockMvc.perform(head("/192.0.2.3")).andExpect(status().isServiceUnavailable());
    }
}
