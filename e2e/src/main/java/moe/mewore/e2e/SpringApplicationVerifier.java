package moe.mewore.e2e;

import moe.mewore.e2e.output.ApplicationLogOutput;
import moe.mewore.e2e.output.ApplicationOutput;
import moe.mewore.e2e.tracking.SpringOutputTracker;
import moe.mewore.e2e.tracking.TomcatStartEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.time.Duration;
import java.util.Optional;
import java.util.Scanner;

public class SpringApplicationVerifier {

    private static final int SECONDS_TO_WAIT = 30;
    private static final Duration WAIT_TIMEOUT = Duration.ofSeconds(SECONDS_TO_WAIT);

    private static final Logger LOGGER = LogManager.getLogger(SpringApplicationVerifier.class);

    public static void main(final String[] args) throws IOException, InterruptedException {
        try {
            new SpringApplicationVerifier().verify();
        } catch (final IOException | RuntimeException e) {
            LOGGER.error("HTML application verification failed!", e);
            throw e;
        }
    }

    private void verify() throws IOException, InterruptedException {
        final @Nullable String logFilePath = System.getenv("LOG_FILE");
        if (logFilePath != null) {
            final File logFile = new File(logFilePath);
            LOGGER.info("Using the 'LOG_FILE' environment variable: " + logFilePath);
            try (final ApplicationOutput applicationOutput = new ApplicationLogOutput(logFile)) {
                verify(applicationOutput);
                return;
            }
        }

        final String protocol = Optional.ofNullable(System.getenv("PROTOCOL")).orElseGet(() -> {
            LOGGER.warn("The 'PROTOCOL' environment variable has not been set; assuming it's 'http'");
            return "http";
        });
        final int port = Optional.ofNullable(System.getenv("PORT"))
                .map(Integer::parseInt)
                .orElseThrow(() -> new IllegalStateException("No 'PORT' environment variable has been set!"));
        verify(protocol, port);
    }

    public void verify(final ApplicationOutput applicationOutput) throws IOException, InterruptedException {
        try (final SpringOutputTracker tracker = new SpringOutputTracker(applicationOutput)) {
            final boolean initialized = Optional.ofNullable(tracker.waitForInitialization(WAIT_TIMEOUT))
                    .orElseThrow(() -> new IllegalStateException(
                            "The application was not initialized in " + SECONDS_TO_WAIT + " seconds!"));
            if (!initialized) {
                throw new IllegalStateException("The application failed to start");
            }
            final TomcatStartEvent tomcatStart = Optional.ofNullable(tracker.waitForTomcatStart(WAIT_TIMEOUT))
                    .orElseThrow(
                            () -> new IllegalStateException("Tomcat did not start in " + SECONDS_TO_WAIT + " seconds"));
            verify(tomcatStart.getProtocol(), tomcatStart.getPort());
        }
    }

    public void verify(final String protocol, final int port) throws IOException {
        final String targetUrl = protocol + "://localhost:" + port;
        LOGGER.info("Verifying HTML application running at: " + targetUrl);
        verifyResponses(targetUrl, "hi im mewore");
        verifyResponses(targetUrl + "/rabbits", "bnuy");
    }

    private void verifyResponses(final String targetUrl, final String expectedResponse) throws IOException {
        final URLConnection connection = new URL(targetUrl).openConnection();
        final Scanner scanner = new Scanner(connection.getInputStream());
        scanner.useDelimiter("\\Z");
        final String content = scanner.next();
        scanner.close();
        if (content.contains(expectedResponse)) {
            LOGGER.info("Got the expected response '" + expectedResponse + "' from " + targetUrl);
        } else {
            LOGGER.error("Did not get the expected response '" + expectedResponse + "' from " + targetUrl +
                    "\nThe actual response was:\n" + content);
            throw new IllegalStateException("Did not get the expected response from " + targetUrl);
        }
    }
}
