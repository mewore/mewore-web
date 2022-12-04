package moe.mewore.web.services.rabbit;

import moe.mewore.web.services.util.FileService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RabbitHtmlTemplateServiceTest {

    @InjectMocks
    private RabbitHtmlTemplateService rabbitHtmlTemplateService;

    @Mock
    private FileService fileService;

    @Mock
    private RabbitSettingsService rabbitSettingsService;

    @BeforeEach
    void setUp() {
        when(rabbitSettingsService.getRabbitPageFilename()).thenReturn("template.html");
    }

    @Test
    void testGetRabbitHtmlTemplateReader_fileLocation() throws IOException {
        when(rabbitSettingsService.getStaticLocations()).thenReturn("file:location");

        final File fileMock = mock(File.class);
        when(fileMock.isFile()).thenReturn(true);
        when(fileService.find(eq(Path.of("location")), eq("template.html"))).thenReturn(fileMock);

        final RabbitHtmlTemplateService.FileReader result = rabbitHtmlTemplateService.getRabbitHtmlTemplateReader();
        Assertions.assertNotNull(result);

        when(fileService.readLines(fileMock)).thenReturn(List.of("1", "2"));
        Assertions.assertArrayEquals(new String[]{"1", "2"}, result.read().toArray(new String[0]));
        Assertions.assertSame(result, rabbitHtmlTemplateService.getRabbitHtmlTemplateReader());
    }

    @Test
    void testGetRabbitHtmlTemplateReader_fileLocation_notFile() {
        when(rabbitSettingsService.getStaticLocations()).thenReturn("file:location");

        final File fileMock = mock(File.class);
        when(fileMock.isFile()).thenReturn(false);
        when(fileService.find(eq(Path.of("location")), eq("template.html"))).thenReturn(fileMock);
        Assertions.assertNull(rabbitHtmlTemplateService.getRabbitHtmlTemplateReader());
    }

    @Test
    void testGetRabbitHtmlTemplateReader_fileLocation_notFound() {
        when(rabbitSettingsService.getStaticLocations()).thenReturn("file:location");
        when(fileService.find(eq(Path.of("location")), eq("template.html"))).thenReturn(null);
        Assertions.assertNull(rabbitHtmlTemplateService.getRabbitHtmlTemplateReader());
    }

    @Test
    void testGetRabbitHtmlTemplateReader_classpathLocation() throws IOException {
        when(rabbitSettingsService.getStaticLocations()).thenReturn("classpath:/location/");

        when(fileService.readLinesFromResource("location/template.html")).thenReturn(List.of("1", "2"));
        final RabbitHtmlTemplateService.FileReader result = rabbitHtmlTemplateService.getRabbitHtmlTemplateReader();
        Assertions.assertNotNull(result);

        Assertions.assertArrayEquals(new String[]{"1", "2"}, result.read().toArray(new String[0]));
        Assertions.assertSame(result, rabbitHtmlTemplateService.getRabbitHtmlTemplateReader());
    }

    @Test
    void testGetRabbitHtmlTemplateReader_classpathLocation_unableToRead() throws IOException {
        when(rabbitSettingsService.getStaticLocations()).thenReturn("classpath:/location/");

        when(fileService.readLinesFromResource("location/template.html")).thenReturn(null);
        final RabbitHtmlTemplateService.FileReader result = rabbitHtmlTemplateService.getRabbitHtmlTemplateReader();
        Assertions.assertNull(result);
    }

    @Test
    void testGetRabbitHtmlTemplateReader_classpathLocation_exception() throws IOException {
        when(rabbitSettingsService.getStaticLocations()).thenReturn("classpath:/location/");

        when(fileService.readLinesFromResource("location/template.html")).thenThrow(new IOException("oof"));
        final RabbitHtmlTemplateService.FileReader result = rabbitHtmlTemplateService.getRabbitHtmlTemplateReader();
        Assertions.assertNull(result);
    }
}