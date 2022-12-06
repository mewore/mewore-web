package moe.mewore.web.services.rabbit;

import lombok.RequiredArgsConstructor;
import moe.mewore.imagediary.ImageDay;
import moe.mewore.imagediary.ImageDiary;
import moe.mewore.web.services.util.TimeService;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class RabbitDiaryRefreshService {

    private final RabbitDiaryService rabbitDiaryService;

    private final TimeService timeService;

    private final RabbitSettingsService rabbitSettingsService;

    private final TaskExecutor taskExecutor;

    @PostConstruct
    void setUp() {
        taskExecutor.execute(this::keepRabbitDaysUpToDate);
    }

    private void keepRabbitDaysUpToDate() {
        final ImageDiary rabbitDiary = rabbitDiaryService.getRabbitDiary();
        final List<ImageDay> days = rabbitDiary.getDaysReversed();
        try {
            for (final ImageDay day : days) {
                try {
                    day.refresh();
                    timeService.sleep(rabbitSettingsService.getAutoRefreshDelayMs(), TimeUnit.MILLISECONDS);
                } catch (final RuntimeException e) {
                    e.printStackTrace();
                }
            }
            if (days.isEmpty()) {
                timeService.sleep(rabbitSettingsService.getAutoRefreshDelayMs(), TimeUnit.MILLISECONDS);
            }
        } catch (final InterruptedException e) {
            System.out.println("The rabbit background refresher has been interrupted.");
            Thread.currentThread().interrupt();
            return;
        }
        System.out.printf("Rabbit diary size: %d MB (%d days)%n", rabbitDiary.getSize() / 1024L / 1024L, days.size());
        taskExecutor.execute(this::keepRabbitDaysUpToDate);
    }
}
