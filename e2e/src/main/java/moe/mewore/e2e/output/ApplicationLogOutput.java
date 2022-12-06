package moe.mewore.e2e.output;

import lombok.RequiredArgsConstructor;
import lombok.Synchronized;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

@RequiredArgsConstructor
public class ApplicationLogOutput implements ApplicationOutput, AutoCloseable {

    private final Logger logger = LogManager.getLogger(getClass());

    private final File file;

    private WatchService watcher;

    private InputStream inputStream;

    private final BlockingQueue<String> fileLines = new LinkedBlockingDeque<>();

    private Thread watchThread;

    private boolean mayHaveNextLine = true;

    private StringBuilder lineBuilder = new StringBuilder();

    @Synchronized
    private void watchFile() throws IOException {
        if (watchThread != null) {
            return;
        }
        if (watcher == null) {
            watcher = FileSystems.getDefault().newWatchService();
        }
        final Path dir = file.toPath().toAbsolutePath().getParent();
        logger.info("Watching directory: " + dir.toFile().getAbsolutePath());
        if (file.exists()) {
            logger.info("File " + file.getAbsolutePath() + " already exists");
            dir.register(watcher, StandardWatchEventKinds.ENTRY_MODIFY);
            initInputStream();
            processInput();
        } else {
            logger.info("File " + file.getAbsolutePath() + " does not exist (yet?)");
            dir.register(watcher, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY);
        }
        logger.info("Initializing thread...");
        watchThread = new Thread(() -> {
            try {
                while (mayHaveNextLine) {
                    mayHaveNextLine = checkForUpdates();
                }
            } finally {
                mayHaveNextLine = false;
            }
        });
        watchThread.start();
    }

    private boolean checkForUpdates() {
        final WatchKey key;
        try {
            key = watcher.take();
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } catch (final ClosedWatchServiceException e) {
            return false;
        }

        for (final WatchEvent<?> event : key.pollEvents()) {
            final WatchEvent.Kind<?> kind = event.kind();
            if (kind == StandardWatchEventKinds.OVERFLOW) {
                continue;
            }

            final String filename = ((Path) event.context()).toFile().getName();
            if (!filename.equals(file.getName())) {
                continue;
            }

            try {
                initInputStream();
                processInput();
            } catch (final IOException e) {
                logger.error("Failed to read file " + file.getAbsolutePath(), e);
                return false;
            }
        }

        if (!key.reset()) {
            logger.error("The watch key of file " + file.getAbsolutePath() + " is now invalid");
            return false;
        }
        return true;
    }

    private void processInput() throws IOException {
        final String input = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        final String[] parts = input.split("\n", -1);
        lineBuilder.append(parts[0]);
        for (int index = 1; index < parts.length; index++) {
            fileLines.add(lineBuilder.toString());
            lineBuilder = new StringBuilder(parts[index]);
        }
    }

    @Override
    public String nextLine() throws InterruptedException, IOException {
        watchFile();
        return fileLines.take();
    }

    @Override
    public boolean mayHaveNextLine() {
        return mayHaveNextLine;
    }

    private void initInputStream() throws FileNotFoundException {
        inputStream = inputStream == null ? new FileInputStream(file) : inputStream;
    }

    @Override
    @Synchronized
    public void close() throws IOException {
        mayHaveNextLine = false;
        if (inputStream != null) {
            inputStream.close();
        }
        if (watcher != null) {
            watcher.close();
        }
        watchThread.interrupt();
    }
}
