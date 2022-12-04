package moe.mewore.imagediary;

import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.List;
import java.util.Map;

public interface ImageDiary {

    void refresh();

    List<ImageDay> getDaysReversed();

    List<Map.Entry<String, List<ImageDay>>> getDaysByMonthReversed();

    @Nullable ImageDay getDay(String name);

    /**
     * @return An estimate of the memory this diary takes up in bytes.
     */
    long getSize();
}
