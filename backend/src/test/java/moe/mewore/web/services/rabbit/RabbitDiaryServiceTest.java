package moe.mewore.web.services.rabbit;

import moe.mewore.web.services.util.FileService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RabbitDiaryServiceTest {

    @InjectMocks
    private RabbitDiaryService rabbitDiaryService;

    @SuppressWarnings("unused")
    @Mock
    private FileService fileService;

    @Mock
    private RabbitSettingsService rabbitSettingsService;

    @Test
    void testSetUp() {
        when(rabbitSettingsService.getRabbitDiaryLocation()).thenReturn("nonexistent-location");
        rabbitDiaryService.setUp();
    }

    @Test
    void getRabbitDiary() {
        when(rabbitSettingsService.getRabbitDiaryLocation()).thenReturn("nonexistent-location");
        rabbitDiaryService.setUp();
        Assertions.assertNotNull(rabbitDiaryService.getRabbitDiary());
    }

    @Test
    void getRabbitDiary_notSetUp() {
        Assertions.assertNull(rabbitDiaryService.getRabbitDiary());
    }
}