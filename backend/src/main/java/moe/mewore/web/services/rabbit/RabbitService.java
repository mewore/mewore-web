package moe.mewore.web.services.rabbit;

import lombok.RequiredArgsConstructor;
import moe.mewore.imagediary.ImageDay;
import moe.mewore.web.exceptions.NotFoundException;
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
import java.util.function.Function;
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
        final int monthIndex = month == null ? Math.min(daysByMonth.size() - 1, 0) : getMonthIndex(daysByMonth, month);
        final List<ImageDay> rabbitDays =
                monthIndex < 0 ? Collections.emptyList() : daysByMonth.get(monthIndex).getValue();
        final int futureMonthIndex = monthIndex < 0 ? getGreaterMonthIndex(daysByMonth, month) : monthIndex - 1;
        final Map<String, String> replacements = getHtmlTemplateValues(monthIndex, futureMonthIndex, daysByMonth);

        return htmlFile.read().stream().map(new RabbitTemplateLineMapper(rabbitDays, replacements, rabbitApiEndpoint));
    }

    @RequiredArgsConstructor
    private static class RabbitTemplateLineMapper implements Function<String, String> {

        private boolean isInRabbitDayTemplate = false;

        private final StringBuilder rabbitDayTemplate = new StringBuilder();

        private final List<ImageDay> rabbitDays;

        private final Map<String, String> replacements;

        private final String rabbitApiEndpoint;

        @Override
        public String apply(String line) {

            boolean hasJustFinishedConstructingTemplate = false;
            if (line.endsWith(RABBIT_DAY_MARKER)) {
                isInRabbitDayTemplate = !isInRabbitDayTemplate;
                if (isInRabbitDayTemplate) {
                    return "";
                } else {
                    hasJustFinishedConstructingTemplate = true;
                }
            }
            if (isInRabbitDayTemplate) {
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
        }
    }

    private static String applyDayToTemplate(final ImageDay day, final String template,
            final String rabbitApiEndpoint) {
        final int imageMask = day.getImageMask();
        final String dayUrl = rabbitApiEndpoint + "/" + day.getDate();
        final List<String> links = new ArrayList<>(24);
        for (int hour = 0; hour < 24; hour++) {
            if (((imageMask >> hour) & 1) != 0) {
                links.add(String.format("<a href=\"%s\" target=\"_blank\"></a>", dayUrl + "/" + hour));
            } else {
                links.add("<div></div>");
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

    private static Map<String, String> getHtmlTemplateValues(final int monthIndex, final int futureMonthIndex,
            final List<Map.Entry<String, List<ImageDay>>> daysByMonth) {

        final Map<String, String> result = new HashMap<>();
        result.put("[NEXT_MONTH_NAME]", futureMonthIndex < 0
                ? "No newer rabbits"
                : getMonthDisplayName(daysByMonth.get(futureMonthIndex).getKey()));
        result.put("[CURRENT_MONTH_NAME]",
                monthIndex < 0 ? "No rabbits" : getMonthDisplayName(daysByMonth.get(monthIndex).getKey()));
        final int pastMonthIndex = monthIndex < 0 ? futureMonthIndex + 1 : monthIndex + 1;
        result.put("[PREVIOUS_MONTH_NAME]", pastMonthIndex >= daysByMonth.size()
                ? "No older rabbits"
                : getMonthDisplayName(daysByMonth.get(pastMonthIndex).getKey()));

        final int pastRabbitCount = daysByMonth.subList(pastMonthIndex, daysByMonth.size())
                .parallelStream()
                .mapToInt(a -> a.getValue().parallelStream().mapToInt(ImageDay::getImageCount).sum())
                .sum();
        result.put("[PREVIOUS_RABBITS_COUNT]", String.valueOf(pastRabbitCount));
        final int currentRabbitCount = monthIndex < 0
                ? 0
                : daysByMonth.get(monthIndex).getValue().parallelStream().mapToInt(ImageDay::getImageCount).sum();
        result.put("[CURRENT_RABBITS_COUNT]", String.valueOf(currentRabbitCount));
        final int futureRabbitCount = daysByMonth.subList(0, futureMonthIndex + 1)
                .parallelStream()
                .mapToInt(a -> a.getValue().parallelStream().mapToInt(ImageDay::getImageCount).sum())
                .sum();
        result.put("[NEXT_RABBITS_COUNT]", String.valueOf(futureRabbitCount));
        result.put("PREVIOUS_RABBITS_TAG", pastRabbitCount == 0 ? "span" : "a");
        result.put("NEXT_RABBITS_TAG", futureRabbitCount == 0 ? "span" : "a");

        result.put("[PREVIOUS_RABBITS_MONTH]",
                pastMonthIndex >= daysByMonth.size() ? "" : "?month=" + daysByMonth.get(pastMonthIndex).getKey());
        result.put("[NEXT_RABBITS_MONTH]",
                futureMonthIndex <= 0 ? "" : "?month=" + daysByMonth.get(futureMonthIndex).getKey());

        return result;
    }
}
