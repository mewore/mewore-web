package moe.mewore.web.services.rabbit;

import moe.mewore.web.services.util.FileService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import moe.mewore.imagediary.ImageDiary;
import moe.mewore.imagediary.LocalImageDiary;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.File;

@Service
@RequiredArgsConstructor
public class RabbitDiaryService {

    private final FileService fileService;

    private final RabbitSettingsService rabbitSettingsService;

    @Getter
    private ImageDiary rabbitDiary;

    @PostConstruct
    void setUp() {
        rabbitDiary = new LocalImageDiary(new File[]{new File(rabbitSettingsService.getRabbitDiaryLocation()), new File(
                rabbitSettingsService.getRabbitDiaryLocation() + "/old")}, fileService::readPngImage,
                fileService::readTextFile);
    }
}
