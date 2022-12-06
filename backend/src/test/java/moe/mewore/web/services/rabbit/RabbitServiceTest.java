package moe.mewore.web.services.rabbit;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import moe.mewore.imagediary.ImageDay;
import moe.mewore.imagediary.ImageDiary;
import moe.mewore.web.exceptions.NotFoundException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RabbitServiceTest {

    private static final String[] TEMPLATE_LINES = new String[]{
            "PREVIOUS_MONTH_NAME: [PREVIOUS_MONTH_NAME]",
            "CURRENT_MONTH_NAME: [CURRENT_MONTH_NAME]",
            "NEXT_MONTH_NAME: [NEXT_MONTH_NAME]",
            "PREVIOUS_RABBITS_COUNT: [PREVIOUS_RABBITS_COUNT]",
            "CURRENT_RABBITS_COUNT: [CURRENT_RABBITS_COUNT]",
            "NEXT_RABBITS_COUNT: [NEXT_RABBITS_COUNT]",
            "PREVIOUS RABBITS TAG: PREVIOUS_RABBITS_TAG",
            "NEXT RABBITS TAG: NEXT_RABBITS_TAG",
            "PREVIOUS_RABBITS_MONTH: [PREVIOUS_RABBITS_MONTH]",
            "NEXT_RABBITS_MONTH: [NEXT_RABBITS_MONTH]",
            "<!--RABBIT DAY-->",
            "NAME: [NAME]",
            "THUMBNAIL_URL: [THUMBNAIL_URL]",
            "LINKS: [LINKS]",
            "<!--RABBIT DAY-->",
            ""
    };

    @InjectMocks
    private RabbitService rabbitService;

    @Mock
    private RabbitDiaryService rabbitDiaryService;

    @Mock
    private RabbitHtmlTemplateService rabbitHtmlTemplateService;


    @Test
    void testGetIndexPage() throws NotFoundException, IOException {
        when(rabbitHtmlTemplateService.getRabbitHtmlTemplateReader()).thenReturn(() -> List.of(TEMPLATE_LINES));
        final ImageDiary rabbitDiary = mock(ImageDiary.class);
        when(rabbitDiaryService.getRabbitDiary()).thenReturn(rabbitDiary);
        final List<Map.Entry<String, List<ImageDay>>> days = makeDaysByMonth("2022-12", "2022-11", "2022-10");
        when(rabbitDiary.getDaysByMonthReversed()).thenReturn(days);
        when(days.get(1).getValue().get(0).getDate()).thenReturn("2022-11-01");
        when(days.get(1).getValue().get(0).getImageMask()).thenReturn(1);
        when(days.get(1).getValue().get(1).getDate()).thenReturn("2022-11-02");
        when(days.get(1).getValue().get(1).getImageMask()).thenReturn(1 << 10);

        when(days.get(0).getValue().get(0).getImageCount()).thenReturn(1);

        when(days.get(1).getValue().get(0).getImageCount()).thenReturn(1);
        when(days.get(1).getValue().get(1).getImageCount()).thenReturn(1);

        when(days.get(2).getValue().get(0).getImageCount()).thenReturn(2);
        when(days.get(2).getValue().get(1).getImageCount()).thenReturn(3);
        when(days.get(2).getValue().get(2).getImageCount()).thenReturn(4);

        final String result = rabbitService.getIndexPage("/api/rabbits", "2022-11").collect(Collectors.joining("\n"));
        Assertions.assertEquals("""
                PREVIOUS_MONTH_NAME: October 2022
                CURRENT_MONTH_NAME: November 2022
                NEXT_MONTH_NAME: December 2022
                PREVIOUS_RABBITS_COUNT: 9
                CURRENT_RABBITS_COUNT: 2
                NEXT_RABBITS_COUNT: 1
                PREVIOUS RABBITS TAG: a
                NEXT RABBITS TAG: a
                PREVIOUS_RABBITS_MONTH: ?month=2022-10
                NEXT_RABBITS_MONTH:\s




                NAME: 2022-11-01
                THUMBNAIL_URL: /api/rabbits/2022-11-01/thumbnail.png
                LINKS: <a href="/api/rabbits/2022-11-01/0" target="_blank"></a><div></div><div></div><div></div><div></div><div></div><div></div><div></div><div></div><div></div><div></div><div></div><div></div><div></div><div></div><div></div><div></div><div></div><div></div><div></div><div></div><div></div><div></div><div></div>
                            
                NAME: 2022-11-02
                THUMBNAIL_URL: /api/rabbits/2022-11-02/thumbnail.png
                LINKS: <div></div><div></div><div></div><div></div><div></div><div></div><div></div><div></div><div></div><div></div><a href="/api/rabbits/2022-11-02/10" target="_blank"></a><div></div><div></div><div></div><div></div><div></div><div></div><div></div><div></div><div></div><div></div><div></div><div></div><div></div>
                   """.trim(), result.trim());
    }

    @Test
    void testGetIndexPage_noRabbits() throws NotFoundException, IOException {
        when(rabbitHtmlTemplateService.getRabbitHtmlTemplateReader()).thenReturn(() -> List.of(TEMPLATE_LINES));
        final ImageDiary rabbitDiary = mock(ImageDiary.class);
        when(rabbitDiaryService.getRabbitDiary()).thenReturn(rabbitDiary);
        when(rabbitDiary.getDaysByMonthReversed()).thenReturn(Collections.emptyList());

        final String result = rabbitService.getIndexPage("/api/rabbits", "2022-11").collect(Collectors.joining("\n"));
        Assertions.assertEquals("""
                PREVIOUS_MONTH_NAME: No older rabbits
                CURRENT_MONTH_NAME: No rabbits
                NEXT_MONTH_NAME: No newer rabbits
                PREVIOUS_RABBITS_COUNT: 0
                CURRENT_RABBITS_COUNT: 0
                NEXT_RABBITS_COUNT: 0
                PREVIOUS RABBITS TAG: span
                NEXT RABBITS TAG: span
                PREVIOUS_RABBITS_MONTH:\s
                NEXT_RABBITS_MONTH:\s
                   """.trim(), result.trim());
    }

    @Test
    void testGetIndexPage_noMoreRecent() throws NotFoundException, IOException {
        when(rabbitHtmlTemplateService.getRabbitHtmlTemplateReader()).thenReturn(() -> List.of(TEMPLATE_LINES));
        final ImageDiary rabbitDiary = mock(ImageDiary.class);
        when(rabbitDiaryService.getRabbitDiary()).thenReturn(rabbitDiary);
        final List<Map.Entry<String, List<ImageDay>>> days = makeDaysByMonth("2022-11", "2022-10");
        when(rabbitDiary.getDaysByMonthReversed()).thenReturn(days);
        when(days.get(0).getValue().get(0).getDate()).thenReturn("2022-11-01");
        when(days.get(0).getValue().get(0).getImageMask()).thenReturn(1);

        when(days.get(0).getValue().get(0).getImageCount()).thenReturn(1);

        when(days.get(1).getValue().get(0).getImageCount()).thenReturn(1);
        when(days.get(1).getValue().get(1).getImageCount()).thenReturn(2);

        final String result = rabbitService.getIndexPage("/api/rabbits", "2022-11").collect(Collectors.joining("\n"));
        Assertions.assertEquals("""
                 PREVIOUS_MONTH_NAME: October 2022
                 CURRENT_MONTH_NAME: November 2022
                 NEXT_MONTH_NAME: No newer rabbits
                 PREVIOUS_RABBITS_COUNT: 3
                 CURRENT_RABBITS_COUNT: 1
                 NEXT_RABBITS_COUNT: 0
                 PREVIOUS RABBITS TAG: a
                 NEXT RABBITS TAG: span
                 PREVIOUS_RABBITS_MONTH: ?month=2022-10
                 NEXT_RABBITS_MONTH:\s




                 NAME: 2022-11-01
                 THUMBNAIL_URL: /api/rabbits/2022-11-01/thumbnail.png
                 LINKS: <a href="/api/rabbits/2022-11-01/0" target="_blank"></a><div></div><div></div><div></div><div></div><div></div><div></div><div></div><div></div><div></div><div></div><div></div><div></div><div></div><div></div><div></div><div></div><div></div><div></div><div></div><div></div><div></div><div></div><div></div>
                   """.trim(), result.trim());
    }

    @Test
    void testGetIndexPage_noOlder() throws NotFoundException, IOException {
        when(rabbitHtmlTemplateService.getRabbitHtmlTemplateReader()).thenReturn(() -> List.of(TEMPLATE_LINES));
        final ImageDiary rabbitDiary = mock(ImageDiary.class);
        when(rabbitDiaryService.getRabbitDiary()).thenReturn(rabbitDiary);
        final List<Map.Entry<String, List<ImageDay>>> days = makeDaysByMonth("2022-12", "2022-11", "2022-10");
        when(rabbitDiary.getDaysByMonthReversed()).thenReturn(days);
        when(days.get(2).getValue().get(0).getDate()).thenReturn("2022-10-01");
        when(days.get(2).getValue().get(1).getDate()).thenReturn("2022-10-02");
        when(days.get(2).getValue().get(2).getDate()).thenReturn("2022-10-03");

        when(days.get(0).getValue().get(0).getImageCount()).thenReturn(1);

        when(days.get(1).getValue().get(0).getImageCount()).thenReturn(1);
        when(days.get(1).getValue().get(1).getImageCount()).thenReturn(2);

        final String result = rabbitService.getIndexPage("/api/rabbits", "2022-10").collect(Collectors.joining("\n"));
        Assertions.assertEquals("""
                PREVIOUS_MONTH_NAME: No older rabbits
                CURRENT_MONTH_NAME: October 2022
                NEXT_MONTH_NAME: November 2022
                PREVIOUS_RABBITS_COUNT: 0
                CURRENT_RABBITS_COUNT: 0
                NEXT_RABBITS_COUNT: 4
                PREVIOUS RABBITS TAG: span
                NEXT RABBITS TAG: a
                PREVIOUS_RABBITS_MONTH:\s
                NEXT_RABBITS_MONTH: ?month=2022-11




                NAME: 2022-10-01
                THUMBNAIL_URL: /api/rabbits/2022-10-01/thumbnail.png
                LINKS: <div></div><div></div><div></div><div></div><div></div><div></div><div></div><div></div><div></div><div></div><div></div><div></div><div></div><div></div><div></div><div></div><div></div><div></div><div></div><div></div><div></div><div></div><div></div><div></div>

                NAME: 2022-10-02
                THUMBNAIL_URL: /api/rabbits/2022-10-02/thumbnail.png
                LINKS: <div></div><div></div><div></div><div></div><div></div><div></div><div></div><div></div><div></div><div></div><div></div><div></div><div></div><div></div><div></div><div></div><div></div><div></div><div></div><div></div><div></div><div></div><div></div><div></div>

                NAME: 2022-10-03
                THUMBNAIL_URL: /api/rabbits/2022-10-03/thumbnail.png
                LINKS: <div></div><div></div><div></div><div></div><div></div><div></div><div></div><div></div><div></div><div></div><div></div><div></div><div></div><div></div><div></div><div></div><div></div><div></div><div></div><div></div><div></div><div></div><div></div><div></div>
                   """.trim(), result.trim());
    }

    @Test
    void testGetIndexPage_noSuchMonth() throws NotFoundException, IOException {
        when(rabbitHtmlTemplateService.getRabbitHtmlTemplateReader()).thenReturn(() -> List.of(TEMPLATE_LINES));
        final ImageDiary rabbitDiary = mock(ImageDiary.class);
        when(rabbitDiaryService.getRabbitDiary()).thenReturn(rabbitDiary);
        final List<Map.Entry<String, List<ImageDay>>> days = makeDaysByMonth("2022-11");
        when(rabbitDiary.getDaysByMonthReversed()).thenReturn(days);
        when(days.get(0).getValue().get(0).getDate()).thenReturn("2022-11-01");

        final String result = rabbitService.getIndexPage("/api/rabbits", "2022-11").collect(Collectors.joining("\n"));
        Assertions.assertEquals("""
                PREVIOUS_MONTH_NAME: No older rabbits
                CURRENT_MONTH_NAME: November 2022
                NEXT_MONTH_NAME: No newer rabbits
                PREVIOUS_RABBITS_COUNT: 0
                CURRENT_RABBITS_COUNT: 0
                NEXT_RABBITS_COUNT: 0
                PREVIOUS RABBITS TAG: span
                NEXT RABBITS TAG: span
                PREVIOUS_RABBITS_MONTH:\s
                NEXT_RABBITS_MONTH:\s



                
                NAME: 2022-11-01
                THUMBNAIL_URL: /api/rabbits/2022-11-01/thumbnail.png
                LINKS: <div></div><div></div><div></div><div></div><div></div><div></div><div></div><div></div><div></div><div></div><div></div><div></div><div></div><div></div><div></div><div></div><div></div><div></div><div></div><div></div><div></div><div></div><div></div><div></div>

                   """.trim(), result.trim());
    }

    @Test
    void testGetIndexPage_noMonths() throws NotFoundException, IOException {
        when(rabbitHtmlTemplateService.getRabbitHtmlTemplateReader()).thenReturn(() -> List.of(TEMPLATE_LINES));
        final ImageDiary rabbitDiary = mock(ImageDiary.class);
        when(rabbitDiaryService.getRabbitDiary()).thenReturn(rabbitDiary);
        when(rabbitDiary.getDaysByMonthReversed()).thenReturn(Collections.emptyList());

        final String result = rabbitService.getIndexPage("/api/rabbits", "2022-10").collect(Collectors.joining("\n"));
        Assertions.assertEquals("""
                PREVIOUS_MONTH_NAME: No older rabbits
                CURRENT_MONTH_NAME: No rabbits
                NEXT_MONTH_NAME: No newer rabbits
                PREVIOUS_RABBITS_COUNT: 0
                CURRENT_RABBITS_COUNT: 0
                NEXT_RABBITS_COUNT: 0
                PREVIOUS RABBITS TAG: span
                NEXT RABBITS TAG: span
                PREVIOUS_RABBITS_MONTH:\s
                NEXT_RABBITS_MONTH:\s
                   """.trim(), result.trim());
    }

    @Test
    void testGetIndexPage_noHtmlTemplate() {
        when(rabbitHtmlTemplateService.getRabbitHtmlTemplateReader()).thenReturn(null);
        final NotFoundException exception = Assertions.assertThrows(NotFoundException.class,
                () -> rabbitService.getIndexPage("/api/rabbits", "2022-10"));
        Assertions.assertEquals("No rabbit HTML file present", exception.getMessage());
    }

    @Test
     void testGetDayThumbnail() throws NotFoundException {
        final ImageDiary rabbitDiary = mock(ImageDiary.class);
        when(rabbitDiaryService.getRabbitDiary()).thenReturn(rabbitDiary);

        final byte[] data = new byte[0];
        final ImageDay day = mock(ImageDay.class);
        when(rabbitDiary.getDay("2022-12-12")).thenReturn(day);
        when(day.getThumbnailData()).thenReturn(data);

        Assertions.assertSame(data, rabbitService.getDayThumbnail("2022-12-12"));
    }

    @Test
     void testGetDayThumbnail_notFound() {
        final ImageDiary rabbitDiary = mock(ImageDiary.class);
        when(rabbitDiaryService.getRabbitDiary()).thenReturn(rabbitDiary);
        when(rabbitDiary.getDay("2022-12-12")).thenReturn(null);
        final NotFoundException exception = Assertions.assertThrows(NotFoundException.class,
                () -> rabbitService.getDayThumbnail("2022-12-12"));
        Assertions.assertEquals("There is no day with date '2022-12-12'", exception.getMessage());
    }

    @Test
     void testGetDayHourImage() throws NotFoundException {
        final ImageDiary rabbitDiary = mock(ImageDiary.class);
        when(rabbitDiaryService.getRabbitDiary()).thenReturn(rabbitDiary);

        final byte[] data = new byte[0];
        final ImageDay day = mock(ImageDay.class);
        when(rabbitDiary.getDay("2022-12-12")).thenReturn(day);
        when(day.getImageData(10)).thenReturn(data);

        Assertions.assertSame(data, rabbitService.getDayHourImage("2022-12-12", 10));
    }

    @Test
     void testGetDayHourImage_notFound() {
        final ImageDiary rabbitDiary = mock(ImageDiary.class);
        when(rabbitDiaryService.getRabbitDiary()).thenReturn(rabbitDiary);
        when(rabbitDiary.getDay("2022-12-12")).thenReturn(null);

        final NotFoundException exception = Assertions.assertThrows(NotFoundException.class,
                () -> rabbitService.getDayHourImage("2022-12-12", 10));
        Assertions.assertEquals("There is no day with date '2022-12-12'", exception.getMessage());
    }

    @Test
     void testGetDayHourImage_noSuchHour() {
        final ImageDiary rabbitDiary = mock(ImageDiary.class);
        when(rabbitDiaryService.getRabbitDiary()).thenReturn(rabbitDiary);
        final ImageDay day = mock(ImageDay.class);
        when(rabbitDiary.getDay("2022-12-12")).thenReturn(day);
        when(day.getImageData(10)).thenReturn(null);

        final NotFoundException exception = Assertions.assertThrows(NotFoundException.class,
                () -> rabbitService.getDayHourImage("2022-12-12", 10));
        Assertions.assertEquals("Day '2022-12-12' does not have an hour 10", exception.getMessage());
    }

    private static List<Map.Entry<String, List<ImageDay>>> makeDaysByMonth(final String... months) {
        final List<Map.Entry<String, List<ImageDay>>> daysByMonth = new ArrayList<>();
        int dayCount = 1;
        for (final String month : months) {
            final List<ImageDay> days = new ArrayList<>(dayCount);
            for (int i = 0; i < dayCount; i++) {
                days.add(mock(ImageDay.class));
            }
            daysByMonth.add(new DayByMonth(month, days));
            dayCount++;
        }
        return daysByMonth;
    }

    @RequiredArgsConstructor
    @Getter
    private static class DayByMonth implements Map.Entry<String, List<ImageDay>> {
        private final String key;
        private final List<ImageDay> value;

        @Override
        public List<ImageDay> setValue(final List<ImageDay> value) {
            return value;
        }
    }
}