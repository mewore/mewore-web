package moe.mewore.imagediary;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.Synchronized;
import org.checkerframework.checker.nullness.qual.Nullable;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.*;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class LocalImageDay implements ImageDay {

    private static final long REFRESH_COOLDOWN_MS = TimeUnit.MINUTES.toMillis(1);

    public static final int THUMBNAIL_WIDTH = 96;
    public static final int THUMBNAIL_HEIGHT = 128;
    private static final int[] THUMBNAIL_ZEROES = new int[THUMBNAIL_WIDTH * THUMBNAIL_HEIGHT];

    private static final Predicate<String> DIRECTORY_NAME_TEST =
            Pattern.compile("\\d{4}-\\d{2}-\\d{2}").asMatchPredicate();

    private static final Pattern IMAGE_NAME_PATTERN = Pattern.compile("^.+-(\\d{4}-\\d{2}-\\d{2})-(\\d\\d?)\\.png$");

    private static final Pattern EVENT_NAME_PATTERN = Pattern.compile("event-(\\d{4}-\\d{2}-\\d{2})-(\\d\\d?)\\.txt$");

    private final File dayDirectory;

    private final ReadFileFunction<BufferedImage> readImageFunction;

    private final ReadFileFunction<String> readTextFileFunction;

    private final File[] imageFiles = new File[24];
    private final byte[][] imageData = new byte[24][];
    private final long[] imageFilesLastModified = new long[24];
    private byte[] thumbnailData;
    private final BufferedImage thumbnail =
            new BufferedImage(THUMBNAIL_WIDTH * 24, THUMBNAIL_HEIGHT, BufferedImage.TYPE_INT_ARGB);
    private final Graphics2D thumbnailGraphics = thumbnail.createGraphics();

    private final AtomicInteger eventMask = new AtomicInteger();
    private final String[] events = new String[24];

    private long lastRefreshedImagesAt = 0L;
    private long lastRefreshedEventsAt = 0L;

    @Override
    public synchronized void refresh() {
        refreshImages();
        refreshEvents();
    }

    @Override
    public synchronized void createThumbnailFile() {
        final File thumbnailFile = dayDirectory.toPath().resolve("thumbnail.png").toFile();
        if (!thumbnailFile.isFile()) {
            refresh();
            System.out.println("Saving thumbnail: " + thumbnailFile.getAbsolutePath());
            try {
                ImageIO.write(thumbnail, "png", thumbnailFile);
            } catch (final IOException e) {
                System.err.println("Failed to create thumbnail: " + thumbnailFile.getAbsolutePath());
                e.printStackTrace();
            }
        }
    }

    @Synchronized("imageFiles")
    @Override
    public byte @Nullable [] getImageData(final int hour) {
        refreshImages();
        if (hour < 0 || hour >= imageFiles.length) {
            throw new IllegalArgumentException(
                    "The hour " + hour + " is outside of the allowed range: 0-" + (imageFiles.length - 1));
        }
        return imageData[hour];
    }

    @Synchronized("events")
    @Override
    public @Nullable String getEvent(final int hour) {
        refreshEvents();
        if (hour < 0 || hour >= events.length) {
            throw new IllegalArgumentException(
                    "The hour " + hour + " is outside of the allowed range: 0-" + (events.length - 1));
        }
        return events[hour];
    }

    @Synchronized("imageFiles")
    private void refreshImages() {
        final long now = System.currentTimeMillis();
        if (now < lastRefreshedImagesAt + REFRESH_COOLDOWN_MS) {
            return;
        }
        lastRefreshedImagesAt = System.currentTimeMillis();
        final AtomicInteger imageMask = new AtomicInteger(0);
        final File @Nullable [] newImageFiles = dayDirectory.listFiles(this::isImage);
        if (newImageFiles == null || newImageFiles.length == 0) {
            System.err.println("Could not fetch any images in image day directory " + dayDirectory.getAbsolutePath());
        } else {
            Arrays.stream(newImageFiles).parallel().forEach(imageFile -> registerImage(imageFile, imageMask));
        }
        for (int i = 0; i < 24; i++) {
            if ((imageMask.get() & (1 << i)) == 0) {
                imageFiles[i] = null;
                imageFilesLastModified[i] = 0;
                thumbnail.setRGB(i * THUMBNAIL_WIDTH, 0, THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT, THUMBNAIL_ZEROES, 0, 0);
            }
        }
        thumbnailData = readImageData(thumbnail);
    }

    private void registerImage(final File imageFile, final AtomicInteger imageMask) {
        final Matcher nameMatcher = IMAGE_NAME_PATTERN.matcher(imageFile.getName());
        if (!nameMatcher.find()) {
            System.err.println("WTF?! File " + imageFile.getName() + " does not match the image file pattern?!");
            return;
        }
        final int index = Integer.parseInt(nameMatcher.group(2));
        final long lastModified = imageFile.lastModified();
        imageMask.updateAndGet(value -> value | (1 << index));
        if (imageFiles[index] != null) {
            if (imageFilesLastModified[index] == lastModified) {
                return;
            } else {
                thumbnail.setRGB(THUMBNAIL_WIDTH * index, 0, THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT, THUMBNAIL_ZEROES, 0, 0);
            }
        }
        final BufferedImage image;
        try {
            image = readImageFunction.apply(imageFile);
        } catch (final IOException e) {
            e.printStackTrace();
            return;
        }
        imageFiles[index] = imageFile;
        imageData[index] = readImageData(image);
        imageFilesLastModified[index] = lastModified;
        final double horizontalScale = (double) THUMBNAIL_WIDTH / image.getWidth();
        final double verticalScale = (double) THUMBNAIL_HEIGHT / image.getHeight();
        final double scale = Math.max(horizontalScale, verticalScale);
        final BufferedImage tmpThumbnail =
                new BufferedImage((int) (THUMBNAIL_WIDTH / scale), (int) (THUMBNAIL_HEIGHT / scale),
                        BufferedImage.TYPE_INT_RGB);
        final AffineTransform emptyTransform = new AffineTransform(1, 0, 0, 1, 0, 0);
        final AffineTransformOp emptyTransformOp =
                new AffineTransformOp(emptyTransform, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
        tmpThumbnail.createGraphics()
                .drawImage(image, emptyTransformOp, (tmpThumbnail.getWidth() - image.getWidth()) / 2,
                        (tmpThumbnail.getHeight() - image.getHeight()) / 2);

        final AffineTransform transform = new AffineTransform(scale, 0, 0, scale, 0, 0);
        final AffineTransformOp transformOp = new AffineTransformOp(transform, AffineTransformOp.TYPE_BICUBIC);
        thumbnailGraphics.drawImage(tmpThumbnail, transformOp, THUMBNAIL_WIDTH * index, 0);
    }

    private static byte @Nullable [] readImageData(final BufferedImage image) {
        final ByteArrayOutputStream thumbnailBaos = new ByteArrayOutputStream();
        try {
            ImageIO.write(image, "png", thumbnailBaos);
            return thumbnailBaos.toByteArray();
        } catch (final IOException e) {
            e.printStackTrace();
        } catch (final IllegalStateException e) {
            if (!"Shutdown in progress".equals(e.getMessage())) {
                throw e;
            }
        }
        return null;
    }

    @Synchronized("events")
    private void refreshEvents() {
        final long now = System.currentTimeMillis();
        if (now < lastRefreshedEventsAt + REFRESH_COOLDOWN_MS) {
            return;
        }
        lastRefreshedEventsAt = System.currentTimeMillis();
        final File @Nullable [] eventFiles = dayDirectory.listFiles(this::isEvent);
        if (eventFiles != null && eventFiles.length > 0) {
            Arrays.stream(eventFiles).parallel().forEach(this::registerEvent);
        }
        for (int i = 0; i < 24; i++) {
            if ((eventMask.get() & (1 << i)) == 0) {
                events[i] = null;
            }
        }
    }

    private void registerEvent(final File eventFile) {
        final Matcher nameMatcher = EVENT_NAME_PATTERN.matcher(eventFile.getName());
        if (!nameMatcher.find()) {
            System.err.println("WTF?! File " + eventFile.getName() + " does not match the event file pattern?!");
            return;
        }
        final int index = Integer.parseInt(nameMatcher.group(2));
        if (events[index] == null) {
            try {
                events[index] = readTextFileFunction.apply(eventFile);
            } catch (final IOException e) {
                e.printStackTrace();
                return;
            }
            eventMask.updateAndGet(value -> value | (1 << index));
        }
    }

    @Synchronized("imageFiles")
    @Override
    public byte[] getThumbnailData() {
        refresh();
        return thumbnailData;
    }

    @Override
    public int getImageMask() {
        final File @Nullable [] currentImageFiles = dayDirectory.listFiles(this::isImage);
        if (currentImageFiles == null || currentImageFiles.length == 0) {
            System.err.println("Could not fetch any images in image day directory " + dayDirectory.getAbsolutePath());
            return 0;
        }
        int imageMask = 0;
        for (final File imageFile : currentImageFiles) {
            final Matcher nameMatcher = IMAGE_NAME_PATTERN.matcher(imageFile.getName());
            if (nameMatcher.find()) {
                imageMask |= (1 << Integer.parseInt(nameMatcher.group(2)));
            }
        }
        return imageMask;
    }

    @Override
    public int getImageCount() {
        return Integer.bitCount(getImageMask());
    }

    @Synchronized("events")
    @Override
    public int getEventMask() {
        refresh();
        return eventMask.get();
    }

    @Override
    public String getDate() {
        return dayDirectory.getName();
    }

    @Override
    public String getMonth() {
        final String date = getDate();
        return date.substring(0, date.lastIndexOf('-'));
    }

    @Override
    public long getSize() {
        return Arrays.stream(imageData)
                .mapToLong(a -> a == null ? 0L : a.length)
                .sum() + THUMBNAIL_WIDTH * THUMBNAIL_HEIGHT * 4 * 2 + getDate().length() * 2L + 100;
    }

    @Override
    public int compareDate(final ImageDay other) {
        return getDate().compareTo(other.getDate());
    }

    private boolean isEvent(final File file) {
        return matchesFilePattern(file, EVENT_NAME_PATTERN);
    }

    private boolean isImage(final File file) {
        return matchesFilePattern(file, IMAGE_NAME_PATTERN);
    }

    private boolean matchesFilePattern(final File file, final Pattern pattern) {
        if (!file.isFile()) {
            return false;
        }
        final Matcher nameMatcher = pattern.matcher(file.getName());
        if (!nameMatcher.find()) {
            return false;
        }
        return dayDirectory.getName().equals(nameMatcher.group(1)) && isHourIndex(nameMatcher.group(2));
    }

    private static boolean isHourIndex(final String str) {
        final int value;
        try {
            value = Integer.parseInt(str);
        } catch (final NumberFormatException e) {
            return false;
        }
        return value >= 0 && value < 24;
    }

    static boolean isDayDirectory(final File file) {
        return file.isDirectory() && DIRECTORY_NAME_TEST.test(file.getName());
    }
}
