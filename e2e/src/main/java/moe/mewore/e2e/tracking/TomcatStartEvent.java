package moe.mewore.e2e.tracking;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RequiredArgsConstructor
@Getter
public class TomcatStartEvent {

    private final int port;
    private final String protocol;

    private static final Pattern TOMCAT_START_PATTERN =
            Pattern.compile("Tomcat started on port\\(s\\): (\\d+) \\((https?)\\)");

    public static @Nullable TomcatStartEvent fromLine(final String line) {
        final Matcher tomcatStartMatcher = TOMCAT_START_PATTERN.matcher(line);
        if (tomcatStartMatcher.find()) {
            return new TomcatStartEvent(Integer.parseInt(tomcatStartMatcher.group(1)), tomcatStartMatcher.group(2));
        }
        return null;
    }
}
