package moe.mewore.e2e;

import moe.mewore.e2e.output.ApplicationLogOutput;
import moe.mewore.e2e.output.ApplicationOutput;
import moe.mewore.e2e.output.ProcessOutput;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class SpringJarVerifier {

    private static final Logger LOGGER = LogManager.getLogger(SpringJarVerifier.class);

    public static void main(final String[] args) throws IOException, InterruptedException {
        try {
            new SpringJarVerifier().run(args);
        } catch (final IOException | RuntimeException e) {
            LOGGER.error("JAR application verification failed!", e);
            throw e;
        }
    }

    private void run(final String[] args) throws IOException, InterruptedException {
        if (args.length < 1) {
            throw new IllegalArgumentException("There should be at least 1 argument - the .jar file to run!");
        }
        final File jarFile = new File(args[0]);
        if (!jarFile.isFile()) {
            throw new IllegalArgumentException(jarFile.getAbsolutePath() + " is not a file!");
        }
        final String javaHome = System.getenv("JAVA_HOME");
        if (javaHome == null || javaHome.isBlank()) {
            throw new IllegalStateException("JAVA_HOME is not set!");
        }
        final File linuxJava = Path.of(javaHome, "bin", "java").toFile();
        final File windowsJava = Path.of(javaHome, "bin", "java.exe").toFile();
        if (!linuxJava.canExecute() && !windowsJava.canExecute()) {
            throw new IllegalStateException(
                    String.format("Neither %s nor %s an executable!", linuxJava.getAbsolutePath(),
                            windowsJava.getAbsolutePath()));
        }
        final File javaToUse = linuxJava.canExecute() ? linuxJava : windowsJava;
        final List<String> commandParts =
                Arrays.stream(new String[]{javaToUse.getAbsolutePath(), "-jar"}).collect(Collectors.toList());
        commandParts.addAll(Arrays.stream(args).collect(Collectors.toList()));

        LOGGER.info("Executing: " + String.join(" ", commandParts));
        final Process process = new ProcessBuilder().command(commandParts.toArray(String[]::new))
                .redirectError(ProcessBuilder.Redirect.INHERIT)
                .start();

        LOGGER.info("Spawned process with ID: " + process.pid());
        try (final ApplicationOutput applicationOutput = new ProcessOutput(process)) {
            new SpringApplicationVerifier().verify(applicationOutput);
        } finally {
            process.destroy();
            if (process.waitFor(10L, TimeUnit.SECONDS)) {
                LOGGER.info("Process exited with code: " + process.waitFor());
            } else {
                LOGGER.error(
                        "Failed to stop process! Trying to terminate process with ID: " + process.destroyForcibly()
                                .pid());
            }
        }
    }
}
