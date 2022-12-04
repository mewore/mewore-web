package moe.mewore.web.services.rabbit;

import lombok.RequiredArgsConstructor;
import moe.mewore.web.services.util.FileService;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RabbitHtmlTemplateService {

    private final FileService fileService;

    private final RabbitSettingsService rabbitSettingsService;

    private @Nullable FileReader rabbitHtmlFileReader;

    public synchronized @Nullable FileReader getRabbitHtmlTemplateReader() {
        if (rabbitHtmlFileReader != null) {
            return rabbitHtmlFileReader;
        }
        final String staticLocations = rabbitSettingsService.getStaticLocations();
        for (final String location : staticLocations == null ? new String[]{"."} : staticLocations.split(",")) {
            System.out.printf("Looking in: %s%n", location);
            if (location.startsWith("file:")) {
                final File htmlFile =
                        fileService.find(Path.of(location.substring(5)), rabbitSettingsService.getRabbitPageFilename());
                if (htmlFile != null && htmlFile.isFile()) {
                    return rabbitHtmlFileReader = () -> fileService.readLines(htmlFile);
                }
            } else if (location.startsWith("classpath:")) {
                final String resourcePath = location.substring(11) + rabbitSettingsService.getRabbitPageFilename();
                System.out.println(" - Classpath file: " + resourcePath);
                try {
                    final @Nullable List<String> lines = fileService.readLinesFromResource(resourcePath);
                    if (lines != null) {
                        return rabbitHtmlFileReader = () -> lines;
                    }
                } catch (final IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return rabbitHtmlFileReader;
    }

    @FunctionalInterface
    interface FileReader {

        List<String> read() throws IOException;
    }
}
