package moe.mewore.e2e.tracking;

import lombok.RequiredArgsConstructor;
import lombok.Synchronized;
import moe.mewore.e2e.output.ApplicationOutput;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RequiredArgsConstructor
public class SpringOutputTracker implements AutoCloseable {

    private static final Pattern INITIALIZATION_PATTERN = Pattern.compile("Started Application in");

    private final Logger logger = LogManager.getLogger(getClass());

    private final ApplicationOutput output;

    private final BlockingQueue<TomcatStartEvent> tomcatStartQueue = new ArrayBlockingQueue<>(1);
    private final BlockingQueue<Boolean> initializationResultQueue = new ArrayBlockingQueue<>(1);

    private boolean closed = false;
    private Thread trackingThread;

    @Synchronized
    private void initializeTrackingThread() {
        if (trackingThread == null && !closed) {
            logger.info("Initializing thread...");
            trackingThread = new Thread(() -> {
                while (output.mayHaveNextLine()) {
                    final String line;
                    try {
                        line = output.nextLine();
                    } catch (final InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    } catch (final IOException e) {
                        logger.error("Failed to read next line", e);
                        continue;
                    }
                    System.out.print("| ");
                    System.out.println(line);
                    investigateLine(line);
                }
                logger.info("Reached the end of the Spring output");
                offerDummies();
            });
            trackingThread.start();
        }
    }

    private void investigateLine(final String line) {
        final @Nullable TomcatStartEvent tomcatStart = TomcatStartEvent.fromLine(line);
        if (tomcatStart != null && !tomcatStartQueue.offer(tomcatStart)) {
            logger.warn("A second Tomcat start event has been detected?!");
        }
        final Matcher initMatcher = INITIALIZATION_PATTERN.matcher(line);
        if (initMatcher.find() && !initializationResultQueue.offer(true)) {
            logger.warn("A second initialization marker has been detected?!");
        }
        if (line.contains("APPLICATION FAILED TO START") && !initializationResultQueue.offer(false)) {
            logger.warn("A second initialization marker has been detected?!");
        }
    }

    public @Nullable TomcatStartEvent waitForTomcatStart(final Duration timeout) throws InterruptedException {
        initializeTrackingThread();
        final @Nullable TomcatStartEvent result = tomcatStartQueue.poll(timeout.toMillis(), TimeUnit.MILLISECONDS);
        if (result != null && !tomcatStartQueue.offer(result)) {
            logger.warn("A second Tomcat start event has been detected?!");
        }
        return result;
    }

    public @Nullable Boolean waitForInitialization(final Duration timeout) throws InterruptedException {
        initializeTrackingThread();
        final @Nullable Boolean result = initializationResultQueue.poll(timeout.toMillis(), TimeUnit.MILLISECONDS);
        if (result != null && !initializationResultQueue.offer(result)) {
            logger.warn("A second initialization marker has been detected?!");
        }
        return result;
    }

    @Override
    @Synchronized
    public void close() {
        closed = true;
        trackingThread.interrupt();
        offerDummies();
    }

    private void offerDummies() {
        if (tomcatStartQueue.peek() == null && tomcatStartQueue.offer(new TomcatStartEvent(-1, ""))) {
            logger.warn("Still no tomcat start event; returning a dummy one");
        }
        if (initializationResultQueue.offer(false)) {
            logger.warn("Still no initialization result; returning false");
        }
    }
}
