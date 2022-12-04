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
@WebMvcTest(RabbitPageController.class)
class RabbitPageControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RabbitService rabbitService;

    @Test
    void testGetRabbitPage() throws Exception {
        when(rabbitService.getIndexPage(RabbitController.ENDPOINT, "2022-12")).thenReturn(Stream.of("1", "2"));

        mockMvc.perform(MockMvcRequestBuilders.get("/rabbits?month=2022-12"))
                .andExpect(MockMvcResultMatchers.status().is(HttpStatus.OK.value()))
                .andExpect(MockMvcResultMatchers.content().string("1\n2"));
    }

    @Test
    void testGetRabbitPage_invalidMonth() throws Exception {
        when(rabbitService.getIndexPage(RabbitController.ENDPOINT, null)).thenReturn(Stream.of("1", "2"));

        mockMvc.perform(MockMvcRequestBuilders.get("/rabbits?month=invalid"))
                .andExpect(MockMvcResultMatchers.status().is(HttpStatus.OK.value()))
                .andExpect(MockMvcResultMatchers.content().string("1\n2"));
    }

    @Test
    void testGetRabbitPage_unsetMonth() throws Exception {
        when(rabbitService.getIndexPage(RabbitController.ENDPOINT, null)).thenReturn(Stream.of("1", "2"));

        mockMvc.perform(MockMvcRequestBuilders.get("/rabbits"))
                .andExpect(MockMvcResultMatchers.status().is(HttpStatus.OK.value()))
                .andExpect(MockMvcResultMatchers.content().string("1\n2"));
    }
}