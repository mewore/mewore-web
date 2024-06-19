package moe.mewore.web.rabbit.generator;

import lombok.RequiredArgsConstructor;
import moe.mewore.imagediary.ImageDay;
import moe.mewore.imagediary.ImageDiary;
import moe.mewore.imagediary.LocalImageDiary;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.text.MessageFormat;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RequiredArgsConstructor
public class RabbitGenerator {
    private static final int ARG_COUNT = 3;

    private static final String INDEX_FILE = "index.html";
    private static final String RABBIT_IMAGE_DIR_NAME = "rabbit-drawings";

    private static final String[] ARG_DESCRIPTIONS = {
            "Rabbit diary directory (directory with YYYY-MM-DD directories, each containing rabbit images)",
            "Source directory of the template HTML and other files",
            "Target directory of the generated HTML pages",
    };

    final ImageDiary rabbitDiary;
    private final Path rabbitDiaryDir;
    private final Path htmlSourceDir;
    private final Path htmlTargetDir;

    public static void main(final String[] args) throws IllegalArgumentException, IOException {
        if (args.length < ARG_COUNT) {
            final List<String> argDescLines = new ArrayList<>(ARG_DESCRIPTIONS.length);
            for (int index = 0; index < ARG_DESCRIPTIONS.length; index++) {
                argDescLines.add(String.format("\t%d) %s", index + 1, ARG_DESCRIPTIONS[index]));
            }
            throw new IllegalArgumentException("The number of arguments should be at least " + ARG_COUNT + ":\n" +
                                               String.join("\n", argDescLines));
        }

        int currentArg = 0;

        final File diaryDir = new File(args[currentArg++]);
        if (!diaryDir.isDirectory()) {
            throw new IllegalArgumentException(String.format("Argument [%d] (\"%s\") is invalid because this provided" +
                                                             " value for it is not a directory: %s", currentArg,
                    ARG_DESCRIPTIONS[currentArg - 1], diaryDir.getAbsolutePath()));
        }

        final Path htmlSourceDir = Path.of(args[currentArg++]).toAbsolutePath().normalize();
        if (!htmlSourceDir.toFile().isDirectory()) {
            throw new IllegalArgumentException(String.format("Argument [%d] (\"%s\") is invalid because this provided" +
                                                             " value for it is not a directory: %s", currentArg,
                    ARG_DESCRIPTIONS[currentArg - 1], htmlSourceDir));
        }
        if (!htmlSourceDir.resolve(INDEX_FILE).toFile().isFile()) {
            throw new IllegalArgumentException(String.format("Argument [%d] (\"%s\") is invalid because no HTML " +
                                                             "template file exists here: %s", currentArg,
                    ARG_DESCRIPTIONS[currentArg - 1], htmlSourceDir.resolve(INDEX_FILE)));
        }

        final Path htmlTargetDir = Path.of(args[currentArg++]).toAbsolutePath().normalize();
        if (htmlTargetDir.toFile().isFile()) {
            throw new IllegalArgumentException(String.format("Argument [%d] (\"%s\") is invalid because this provided" +
                                                             " value is a file: %s", currentArg,
                    ARG_DESCRIPTIONS[currentArg - 1], htmlTargetDir));
        }

        if (currentArg != args.length) {
            throw new IllegalArgumentException(MessageFormat.format("The arguments should be {0}. There are redundant" +
                                                                    " arguments: {1}", ARG_COUNT,
                    Arrays.stream(args).skip(currentArg).collect(Collectors.joining(" "))));
        }

        final ImageDiary rabbitDiary = new LocalImageDiary(new File[]{diaryDir}, RabbitGenerator::readPngImage,
                RabbitGenerator::readTextFile);
        new RabbitGenerator(rabbitDiary, diaryDir.toPath(), htmlSourceDir, htmlTargetDir).generate();
    }

    private void generate() throws IOException {
        final List<ImageDay> days = rabbitDiary.getDaysReversed();
        for (final ImageDay day : days) {
            day.createThumbnailFile();
        }
        System.out.printf("Rabbit diary size: %d MB (%d days)%n", rabbitDiary.getSize() / 1024L / 1024L, days.size());

        System.out.println("Deleting: " + htmlTargetDir.toAbsolutePath());
        if (!deleteRecursively(htmlTargetDir.toFile())) {
            throw new RuntimeException("Failed to delete directory " + htmlTargetDir);
        }
        System.out.println("Copying: " + htmlSourceDir.toAbsolutePath() + " -> " + htmlTargetDir.toAbsolutePath());
        createSymlinksToFolderChildren(htmlSourceDir, htmlTargetDir, Set.of(INDEX_FILE));
        final List<String> templateLines = readLines(htmlSourceDir.resolve(INDEX_FILE));
        final List<Map.Entry<String, List<ImageDay>>> daysByMonthReversed = rabbitDiary.getDaysByMonthReversed();
        final List<String> months = daysByMonthReversed.stream().map(Map.Entry::getKey).collect(Collectors.toList());
        System.out.println("Months: " + String.join(", ", months));
        months.stream().parallel().forEach(month -> {
            final Path monthDir = htmlTargetDir.resolve(month);
            if (!monthDir.toFile().mkdir()) {
                System.err.println("Failed to create directory " + monthDir);
                return;
            }
            try {
                Files.write(monthDir.resolve(INDEX_FILE), getIndexPage(templateLines, month),
                        StandardOpenOption.WRITE, StandardOpenOption.CREATE_NEW);
            } catch (final IOException e) {
                System.err.println("Failed to create index file for month " + month + " in directory " + monthDir);
                e.printStackTrace();
            }
        });
        final File mainIndexFile = htmlTargetDir.resolve(INDEX_FILE).toFile();
        if (mainIndexFile.exists() && !mainIndexFile.delete()) {
            throw new RuntimeException(
                    "Failed to ensure that file " + mainIndexFile + " does not exist before creating it!");
        }
        Files.write(mainIndexFile.toPath(), getIndexPage(templateLines, null),
                StandardOpenOption.WRITE, StandardOpenOption.CREATE_NEW);

        // Create a symbolic link to the rabbit diary
        Files.createSymbolicLink(htmlTargetDir.resolve(RABBIT_IMAGE_DIR_NAME), rabbitDiaryDir);
    }

    public void createSymlinksToFolderChildren(final Path source, final Path target,
                                               final Set<String> excluded) throws IOException {
        if (!target.toFile().mkdirs()) {
            throw new RuntimeException("Failed to create directory " + target);
        }
        final File @Nullable [] sourceChildren = source.toFile().listFiles();
        if (sourceChildren == null) {
            System.err.println("The source directory " + source + " contains no files");
            return;
        }
        for (final File child : sourceChildren) {
            if (child.getName().endsWith("~") || excluded.contains(child.getName())) {
                continue;
            }
            Files.createSymbolicLink(target.resolve(child.getName()), child.toPath());
        }
    }

    private static boolean deleteRecursively(final File file) {
        final File @Nullable [] allContents = Files.isSymbolicLink(file.toPath()) ? null : file.listFiles();
        if (allContents != null) {
            Arrays.stream(allContents).parallel().forEach(RabbitGenerator::deleteRecursively);
        }
        return !file.exists() || file.delete();
    }

    private static String readTextFile(final File file) throws IOException {
        return Files.readString(file.toPath());
    }

    private static BufferedImage readPngImage(final File file) throws IOException {
        return ImageIO.read(file);
    }

    private static List<String> readLines(final Path file) throws IOException {
        try (final Stream<String> lineStream = Files.lines(file)) {
            return lineStream.collect(Collectors.toList());
        }
    }


    private static final String RABBIT_DAY_MARKER = "<!--RABBIT DAY-->";

    private List<String> getIndexPage(final List<String> templateLines, final @Nullable String month) {
        // TODO: Add behaviour depending on whether caching is enabled
        final List<Map.Entry<String, List<ImageDay>>> daysByMonth = rabbitDiary.getDaysByMonthReversed();
        final int monthIndex = month == null ? Math.min(daysByMonth.size() - 1, 0) : getMonthIndex(daysByMonth, month);
        final List<ImageDay> rabbitDays = monthIndex < 0 ? Collections.emptyList() :
                daysByMonth.get(monthIndex).getValue();
        final int futureMonthIndex = monthIndex < 0 ? getGreaterMonthIndex(daysByMonth, month) : monthIndex - 1;
        final String pathToRootPage = month == null ? "." : "..";
        final Map<String, String> replacements =
                getHtmlTemplateValues(monthIndex, futureMonthIndex, daysByMonth, pathToRootPage);

        return templateLines.stream()
                .map(new RabbitTemplateLineMapper(rabbitDays, replacements, pathToRootPage))
                .collect(Collectors.toList());
    }

    @RequiredArgsConstructor
    private static class RabbitTemplateLineMapper implements Function<String, String> {

        private boolean isInRabbitDayTemplate = false;

        private final StringBuilder rabbitDayTemplate = new StringBuilder();

        private final List<ImageDay> rabbitDays;

        private final Map<String, String> replacements;

        private final String pathToRootPage;

        @Override
        public String apply(String line) {

            boolean hasJustFinishedConstructingTemplate = false;
            if (line.endsWith(RABBIT_DAY_MARKER)) {
                isInRabbitDayTemplate = !isInRabbitDayTemplate;
                if (isInRabbitDayTemplate) {
                    return "";
                }
                hasJustFinishedConstructingTemplate = true;
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
                    .map(day -> applyDayToTemplate(day, template, pathToRootPage))
                    .collect(Collectors.joining("\n"));
        }
    }

    private static String applyDayToTemplate(final ImageDay day, final String template, final String pathToRootPage) {
        final int imageMask = day.getImageMask();
        final String dayUrl = pathToRootPage + "/" + RABBIT_IMAGE_DIR_NAME + "/" + day.getDate();
        final List<String> links = new ArrayList<>(24);
        for (int hour = 0; hour < 24; hour++) {
            final String event = day.getEvent(hour);
            final List<String> content = new ArrayList<>();
            if (event != null) {
                final String eventUrl = MessageFormat.format("{0}/event-{1}-{2}.txt", dayUrl, day.getDate(), hour);
                content.add(String.format("""
                        <a href="%s" class="event-link" title="%s">Note</a>
                        """.trim(), eventUrl, event.replace("\"", "&quot;")));
            }
            if (((imageMask >> hour) & 1) != 0) {
                final String imageUrl = MessageFormat.format("{0}/rabbit-{1}-{2}.png", dayUrl, day.getDate(), hour);
                final String imageLink = String.format("<a href=\"%s\"></a>", imageUrl);
                // A hack for making sure that the Note link is centered between two image links because links aren't
                // allowed to be inside links for whatever reason.
                if (!content.isEmpty()) {
                    content.add(0, imageLink);
                }
                content.add(imageLink);
            }
            links.add(String.format("<div%s>%s</div>", content.isEmpty() ? " class=\"inactive\"" : "",
                    String.join("", content)));
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

    private static Map<String, String> getHtmlTemplateValues(final int monthIndex, final int futureMonthIndex,
                                                             final List<Map.Entry<String, List<ImageDay>>> daysByMonth,
                                                             final String pathToRootPage) {

        final Map<String, String> result = new HashMap<>();
        result.put("[PATH_TO_RABBIT_ROOT]", pathToRootPage);

        result.put("[NEXT_MONTH_NAME]", futureMonthIndex < 0 ? "No newer rabbits" :
                getMonthDisplayName(daysByMonth.get(futureMonthIndex).getKey()));
        result.put("[CURRENT_MONTH_NAME]", monthIndex < 0 ? "No rabbits" :
                getMonthDisplayName(daysByMonth.get(monthIndex).getKey()));
        final int pastMonthIndex = monthIndex < 0 ? futureMonthIndex + 1 : monthIndex + 1;
        result.put("[PREVIOUS_MONTH_NAME]", pastMonthIndex >= daysByMonth.size() ? "No older rabbits" :
                getMonthDisplayName(daysByMonth.get(pastMonthIndex).getKey()));

        final int pastRabbitCount =
                daysByMonth.subList(pastMonthIndex, daysByMonth.size())
                        .parallelStream()
                        .mapToInt(a -> a.getValue().parallelStream().mapToInt(ImageDay::getImageCount).sum())
                        .sum();
        result.put("[PREVIOUS_RABBITS_COUNT]", String.valueOf(pastRabbitCount));
        final int currentRabbitCount = monthIndex < 0 ? 0 :
                daysByMonth.get(monthIndex).getValue().parallelStream().mapToInt(ImageDay::getImageCount).sum();
        result.put("[CURRENT_RABBITS_COUNT]", String.valueOf(currentRabbitCount));
        final int futureRabbitCount =
                daysByMonth.subList(0, futureMonthIndex + 1)
                        .parallelStream()
                        .mapToInt(a -> a.getValue().parallelStream().mapToInt(ImageDay::getImageCount).sum())
                        .sum();
        result.put("[NEXT_RABBITS_COUNT]", String.valueOf(futureRabbitCount));
        result.put("PREVIOUS_RABBITS_TAG", pastRabbitCount == 0 ? "span" : "a");
        result.put("NEXT_RABBITS_TAG", futureRabbitCount == 0 ? "span" : "a");

        result.put("[PREVIOUS_RABBITS_MONTH]", pastMonthIndex >= daysByMonth.size() ? pathToRootPage :
                pathToRootPage + "/" + daysByMonth.get(pastMonthIndex).getKey() + "/");
        result.put("[NEXT_RABBITS_MONTH]", futureMonthIndex <= 0 ? pathToRootPage :
                pathToRootPage + "/" + daysByMonth.get(futureMonthIndex).getKey() + "/");

        return result;
    }
}