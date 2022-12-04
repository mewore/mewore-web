package moe.mewore.imagediary;

import lombok.RequiredArgsConstructor;
import lombok.Synchronized;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.awt.image.*;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class LocalImageDiary implements ImageDiary {

    private static final long REFRESH_COOLDOWN_MS = TimeUnit.SECONDS.toMillis(5);

    private final File[] locations;

    private final Map<String, ImageDay> days = new ConcurrentHashMap<>();

    private List<Map.Entry<String, List<ImageDay>>> daysByMonthReversed = Collections.emptyList();

    private final ReadFileFunction<BufferedImage> readImageFunction;

    private final ReadFileFunction<String> readTextFileFunction;

    private long lastRefreshedAt = 0L;

    @Synchronized
    @Override
    public void refresh() {
        final long now = System.currentTimeMillis();
        if (now < lastRefreshedAt + REFRESH_COOLDOWN_MS) {
            return;
        }
        lastRefreshedAt = System.currentTimeMillis();
        final List<String> currentDayNames = new ArrayList<>();
        final AtomicBoolean changed = new AtomicBoolean(false);
        for (final File location : locations) {
            final File @Nullable [] foundDirectories = location.listFiles(LocalImageDay::isDayDirectory);
            if (foundDirectories != null) {
                for (final File directory : foundDirectories) {
                    currentDayNames.add(directory.getName());
                    days.computeIfAbsent(directory.getName(), key -> {
                        changed.set(true);
                        return new LocalImageDay(directory, readImageFunction, readTextFileFunction);
                    });
                }
            }
        }
        if (days.keySet().retainAll(currentDayNames)) {
            System.out.println("Some image diary days have been removed. Current image dairy days: " + String.join(", ",
                    days.keySet()));
            changed.set(true);
        }

        if (changed.get()) {
            final Map<String, List<ImageDay>> groupedByMonth = new HashMap<>();
            for (final ImageDay day : days.values()) {
                groupedByMonth.computeIfAbsent(day.getMonth(), key -> new ArrayList<>()).add(day);
            }
            for (final String key : groupedByMonth.keySet().toArray(new String[0])) {
                groupedByMonth.computeIfPresent(key, (k, value) -> value.stream()
                        .sorted((first, second) -> second.compareDate(first))
                        .collect(Collectors.toUnmodifiableList()));
            }
            daysByMonthReversed = groupedByMonth.entrySet()
                    .stream()
                    .sorted((o1, o2) -> o2.getKey().compareTo(o1.getKey()))
                    .collect(Collectors.toUnmodifiableList());
        }
    }

    @Override
    public List<ImageDay> getDaysReversed() {
        refresh();
        return days.values().stream().sorted((first, second) -> second.compareDate(first)).collect(Collectors.toList());
    }

    @Override
    public List<Map.Entry<String, List<ImageDay>>> getDaysByMonthReversed() {
        return daysByMonthReversed;
    }

    @Override
    public @Nullable ImageDay getDay(final String name) {
        refresh();
        return days.get(name);
    }

    @Override
    public long getSize() {
        return days.values().stream().mapToLong(ImageDay::getSize).sum();
    }
}
