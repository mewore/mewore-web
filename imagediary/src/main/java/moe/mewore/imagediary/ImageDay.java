package moe.mewore.imagediary;

import org.checkerframework.checker.nullness.qual.Nullable;

public interface ImageDay {

    void refresh();

    /**
     * Generates the thumbnail only if it doesn't already exist.
     */
    void createThumbnailFile();

    byte @Nullable[] getImageData(int hour);

    @Nullable String getEvent(int hour);

    byte[] getThumbnailData();

    /**
     * @return A 24-bit mask where a 1 is a present image in the hour and a 0 is an absent image.
     */
    int getImageMask();

    /**
     * @return The number of available images.
     */
    int getImageCount();

    /**
     * @return A 24-bit mask where a 1 is a present event in the hour and a 0 is an absent event.
     */
    int getEventMask();

    String getDate();

    String getMonth();

    /**
     * @return An estimate of the memory this day takes up in bytes.
     */
    long getSize();

    int compareDate(ImageDay other);
}
