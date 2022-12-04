package moe.mewore.web.services.rabbit;

import moe.mewore.web.services.util.TimeService;
import moe.mewore.imagediary.ImageDay;
import moe.mewore.imagediary.ImageDiary;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.task.TaskExecutor;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RabbitDiaryRefreshServiceTest {

    @InjectMocks
    private RabbitDiaryRefreshService rabbitDiaryRefreshService;

    @Mock
    private RabbitDiaryService rabbitDiaryService;

    @Mock
    private TimeService timeService;

    @Mock
    private RabbitSettingsService rabbitSettingsService;

    @Mock
    private TaskExecutor taskExecutor;

    @Mock
    private ImageDiary imageDiary;

    @Captor
    private ArgumentCaptor<Runnable> runnableCaptor;

    @Test
    void testSetUp() {
        rabbitDiaryRefreshService.setUp();
        verify(taskExecutor).execute(any());
    }

    @Test
    void testSetUp_keepRabbitDaysUpToDate() throws InterruptedException {
        rabbitDiaryRefreshService.setUp();
        verify(taskExecutor).execute(runnableCaptor.capture());

        when(rabbitDiaryService.getRabbitDiary()).thenReturn(imageDiary);
        final ImageDay firstDay = mock(ImageDay.class);
        final ImageDay secondDay = mock(ImageDay.class);
        when(imageDiary.getDaysReversed()).thenReturn(List.of(firstDay, secondDay));
        when(rabbitSettingsService.getAutoRefreshDelayMs()).thenReturn(88L);
        runnableCaptor.getValue().run();

        verify(firstDay).refresh();
        verify(secondDay).refresh();
        verify(rabbitSettingsService, times(2)).getAutoRefreshDelayMs();
        verify(timeService, times(2)).sleep(88L, TimeUnit.MILLISECONDS);
    }

    @Test
    void testSetUp_keepRabbitDaysUpToDate_RuntimeException() throws InterruptedException {
        rabbitDiaryRefreshService.setUp();
        verify(taskExecutor).execute(runnableCaptor.capture());

        when(rabbitDiaryService.getRabbitDiary()).thenReturn(imageDiary);
        final ImageDay firstDay = mock(ImageDay.class);
        final ImageDay secondDay = mock(ImageDay.class);
        doThrow(new RuntimeException("aaAA")).when(firstDay).refresh();
        when(imageDiary.getDaysReversed()).thenReturn(List.of(firstDay, secondDay));
        runnableCaptor.getValue().run();

        verify(firstDay).refresh();
        verify(secondDay).refresh();
        verify(rabbitSettingsService).getAutoRefreshDelayMs();
        verify(timeService).sleep(anyLong(), any());
    }

    @Test
    void testSetUp_keepRabbitDaysUpToDate_InterruptedException() throws InterruptedException {
        rabbitDiaryRefreshService.setUp();
        verify(taskExecutor).execute(runnableCaptor.capture());

        when(rabbitDiaryService.getRabbitDiary()).thenReturn(imageDiary);
        final ImageDay firstDay = mock(ImageDay.class);
        final ImageDay secondDay = mock(ImageDay.class);
        doThrow(new InterruptedException("aaAA")).when(timeService).sleep(anyLong(), any());
        when(imageDiary.getDaysReversed()).thenReturn(List.of(firstDay, secondDay));
        runnableCaptor.getValue().run();
        Assertions.assertTrue(Thread.currentThread().isInterrupted());

        verify(firstDay).refresh();
        verify(secondDay, never()).refresh();
        verify(rabbitSettingsService).getAutoRefreshDelayMs();
    }
}