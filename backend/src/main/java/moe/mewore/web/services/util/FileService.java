package moe.mewore.web.services.util;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class FileService {

    public @Nullable File find(final Path location, final String name) {
        final File result = location.resolve(name).toFile();
        return result.exists() ? result : null;
    }

    public String readTextFile(final File file) throws IOException {
        return Files.readString(file.toPath());
    }

    public BufferedImage readPngImage(final File file) throws IOException {
        return ImageIO.read(file);
    }

    public List<String> readLines(final File file) throws IOException {
        try (final Stream<String> lineStream = Files.lines(file.toPath())) {
            return lineStream.collect(Collectors.toList());
        }
    }

    public @Nullable List<String> readLinesFromResource(final String resource) throws IOException {
        final @Nullable URL url = getClass().getClassLoader().getResource(resource);
        if (url == null) {
            return null;
        }
        try (final InputStream resourceStream = url.openStream()) {
            final Scanner scanner = new Scanner(resourceStream, StandardCharsets.UTF_8);
            final List<String> result = new ArrayList<>();
            while (scanner.hasNext()) {
                result.add(scanner.nextLine());
            }
            return result;
        }
    }
}
