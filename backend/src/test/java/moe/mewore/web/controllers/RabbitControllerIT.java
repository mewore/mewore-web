package moe.mewore.web.controllers;

import moe.mewore.web.config.TestConfig;
import moe.mewore.web.services.rabbit.RabbitService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.util.stream.Stream;

import static org.mockito.Mockito.when;

@Import(TestConfig.class)
@WebMvcTest(RabbitController.class)
class RabbitControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RabbitService rabbitService;

    @Test
    void testGetRabbitDayThumbnail() throws Exception {
        when(rabbitService.getDayThumbnail("2022-12-12")).thenReturn(new byte[]{0, 1, 2});

        mockMvc.perform(MockMvcRequestBuilders.get("/api/rabbits/2022-12-12/thumbnail.png"))
                .andExpect(MockMvcResultMatchers.status().is(HttpStatus.OK.value()))
                .andExpect(MockMvcResultMatchers.content().bytes(new byte[]{0, 1, 2}));
    }

    @Test
    void testGetRabbitHourImage() throws Exception {
        when(rabbitService.getDayHourImage("2022-12-12", 5)).thenReturn(new byte[]{2, 1, 0});

        mockMvc.perform(MockMvcRequestBuilders.get("/api/rabbits/2022-12-12/5"))
                .andExpect(MockMvcResultMatchers.status().is(HttpStatus.OK.value()))
                .andExpect(MockMvcResultMatchers.content().bytes(new byte[]{2, 1, 0}));
    }
}