package moe.mewore.web.services.rabbit;

import lombok.RequiredArgsConstructor;
import moe.mewore.imagediary.ImageDay;
import moe.mewore.web.exceptions.NotFoundException;
import moe.mewore.web.services.util.FileService;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class RabbitService {

    private static final String RABBIT_DAY_MARKER = "<!--RABBIT DAY-->";

    private final RabbitDiaryService rabbitDiaryService;

    private final RabbitHtmlTemplateService rabbitHtmlTemplateService;

    public Stream<String> getIndexPage(final String rabbitApiEndpoint,
            final @Nullable String month) throws NotFoundException, IOException {
        // TODO: Add behaviour depending on whether caching is enabled
        final RabbitHtmlTemplateService.FileReader htmlFile =
                Optional.ofNullable(rabbitHtmlTemplateService.getRabbitHtmlTemplateReader())
                        .orElseThrow(() -> new NotFoundException("No rabbit HTML file present"));
        final List<Map.Entry<String, List<ImageDay>>> daysByMonth =
                rabbitDiaryService.getRabbitDiary().getDaysByMonthReversed();
        final int monthIndex = month == null ? 0 : getMonthIndex(daysByMonth, month);
        final List<ImageDay> rabbitDays =
                monthIndex < 0 ? Collections.emptyList() : daysByMonth.get(monthIndex).getValue();
        final int previousMonthIndex = monthIndex < 0 ? getGreaterMonthIndex(daysByMonth, month) : monthIndex - 1;
        final Map<String, String> replacements = getHtmlTemplateValues(monthIndex, previousMonthIndex, daysByMonth);

        final StringBuilder rabbitDayTemplate = new StringBuilder();
        final AtomicReference<Boolean> isInRabbitDayTemplate = new AtomicReference<>(false);
        return htmlFile.read().stream().map(line -> {
            boolean hasJustFinishedConstructingTemplate = false;
            if (line.endsWith(RABBIT_DAY_MARKER)) {
                if (isInRabbitDayTemplate.updateAndGet(value -> value == null || !value)) {
                    return "";
                } else {
                    hasJustFinishedConstructingTemplate = true;
                }
            }
            if (isInRabbitDayTemplate.get()) {
                rabbitDayTemplate.append(line).append('\n');
                return "";
            }
            if (!hasJustFinishedConstructingTemplate) {
                for (final Map.Entry<String, String> replacement : replacements.entrySet()) {
                    line = line.replace(replacement.getKey(), replacement.getValue());
                }
                return line;
            }
            final String template = rabbitDayTemplate.toString();
            return rabbitDays.parallelStream()
                    .map(day -> applyDayToTemplate(day, template, rabbitApiEndpoint))
                    .collect(Collectors.joining("\n"));
        });
    }

    private static String applyDayToTemplate(final ImageDay day, final String template,
            final String rabbitApiEndpoint) {
        final int imageMask = day.getImageMask();
        final String dayUrl = rabbitApiEndpoint + "/" + day.getDate();
        final List<String> links = new ArrayList<>(24);
        for (int hour = 0; hour < 24; hour++) {
            if (((imageMask >> hour) & 1) != 0) {
                links.add(String.format("<a class=\"rabbit-link\" href=\"%s\" target=\"_blank\"></a>",
                        dayUrl + "/" + hour));
            } else {
                links.add("<div class=\"rabbit-link\"></div>");
            }
        }
        return template.replace("[NAME]", day.getDate())
                .replace("[THUMBNAIL_URL]", dayUrl + "/thumbnail.png")
                .replace("[LINKS]", String.join("", links));
    }

    private static String getMonthDisplayName(final @NonNull String month) {
        final String[] parts = month.split("-");
        final int monthIndex = Integer.parseInt(parts[1]) - 1;
        final String monthName;
        if (monthIndex < 0 || monthIndex >= 12) {
            monthName = "???";
        } else {
            final Calendar c = Calendar.getInstance();
            c.set(Calendar.MONTH, monthIndex);
            c.set(Calendar.DAY_OF_MONTH, 1);
            monthName = c.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.US);
        }
        return monthName + " " + parts[0];
    }

    private static <T> int getMonthIndex(final List<Map.Entry<String, T>> monthGroupList, final String month) {
        for (int i = 0; i < monthGroupList.size(); i++) {
            if (monthGroupList.get(i).getKey().equals(month)) {
                return i;
            }
        }
        return -1;
    }

    private static <T> int getGreaterMonthIndex(final List<Map.Entry<String, T>> monthGroupList, final String month) {
        for (int i = monthGroupList.size() - 1; i >= 0; i--) {
            if (monthGroupList.get(i).getKey().compareTo(month) > 0) {
                return i;
            }
        }
        return -1;
    }

    public byte[] getDayThumbnail(final String name) throws NotFoundException {
        return Optional.ofNullable(rabbitDiaryService.getRabbitDiary().getDay(name))
                .orElseThrow(() -> new NotFoundException("There is no day with date '" + name + "'"))
                .getThumbnailData();
    }

    public byte[] getDayHourImage(final String name, final int hour) throws NotFoundException {
        final ImageDay day = Optional.ofNullable(rabbitDiaryService.getRabbitDiary().getDay(name))
                .orElseThrow(() -> new NotFoundException("There is no day with date '" + name + "'"));

        return Optional.ofNullable(day.getImageData(hour))
                .orElseThrow(() -> new NotFoundException("Day '" + name + "' does not have an hour " + hour));
    }

    private static Map<String, String> getHtmlTemplateValues(final int monthIndex, final int previousMonthIndex,
            final List<Map.Entry<String, List<ImageDay>>> daysByMonth) {

        final Map<String, String> result = new HashMap<>();
        result.put("[PREVIOUS_MONTH_NAME]", previousMonthIndex < 0
                ? "No newer rabbits"
                : getMonthDisplayName(daysByMonth.get(previousMonthIndex).getKey()));
        result.put("[CURRENT_MONTH_NAME]",
                monthIndex < 0 ? "No rabbits" : getMonthDisplayName(daysByMonth.get(monthIndex).getKey()));
        final int nextMonthIndex = monthIndex < 0 ? previousMonthIndex + 1 : monthIndex + 1;
        result.put("[NEXT_MONTH_NAME]", nextMonthIndex >= daysByMonth.size()
                ? "No older rabbits"
                : getMonthDisplayName(daysByMonth.get(nextMonthIndex).getKey()));

        final int previousRabbitCount = daysByMonth.subList(0, previousMonthIndex + 1)
                .parallelStream()
                .mapToInt(a -> a.getValue().parallelStream().mapToInt(ImageDay::getImageCount).sum())
                .sum();
        result.put("[PREVIOUS_RABBITS_COUNT]", String.valueOf(previousRabbitCount));
        final int currentRabbitCount = monthIndex < 0
                ? 0
                : daysByMonth.get(monthIndex).getValue().parallelStream().mapToInt(ImageDay::getImageCount).sum();
        result.put("[CURRENT_RABBITS_COUNT]", String.valueOf(currentRabbitCount));
        final int nextRabbitCount = daysByMonth.subList(nextMonthIndex, daysByMonth.size())
                .parallelStream()
                .mapToInt(a -> a.getValue().parallelStream().mapToInt(ImageDay::getImageCount).sum())
                .sum();
        result.put("[NEXT_RABBITS_COUNT]", String.valueOf(nextRabbitCount));
        result.put("PREVIOUS_RABBITS_TAG", previousRabbitCount == 0 ? "span" : "a");
        result.put("NEXT_RABBITS_TAG", nextRabbitCount == 0 ? "span" : "a");

        result.put("[PREVIOUS_RABBITS_MONTH]",
                previousMonthIndex <= 0 ? "" : "?month=" + daysByMonth.get(previousMonthIndex).getKey());
        result.put("[NEXT_RABBITS_MONTH]",
                nextMonthIndex >= daysByMonth.size() ? "" : "?month=" + daysByMonth.get(nextMonthIndex).getKey());

        return result;
    }
}
