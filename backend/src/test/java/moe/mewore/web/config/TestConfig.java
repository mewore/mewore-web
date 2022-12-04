package moe.mewore.web.config;

import moe.mewore.web.services.util.FileService;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import static org.mockito.Mockito.mock;

@TestConfiguration
public class TestConfig {

    @Bean
    @Primary
    public FileService mockFileService() {
        return mock(FileService.class);
    }
}
