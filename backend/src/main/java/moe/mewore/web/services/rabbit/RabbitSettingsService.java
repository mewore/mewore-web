package moe.mewore.web.services.rabbit;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Getter
public class RabbitSettingsService {

    @Value("${spring.resources.static-locations}")
    private String staticLocations;

    @Value("${rabbit.diary.location}")
    private String rabbitDiaryLocation;

    @Value("${rabbit.diary.auto-refresh.delay-ms}")
    private long autoRefreshDelayMs;

    @Value("${rabbit.page.path}")
    private String rabbitPageFilename;
}
