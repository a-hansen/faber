package com.comfortanalytics.faber.tools;

import com.comfortanalytics.faber.annotation.Nonnull;
import dev.langchain4j.agent.tool.Tool;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BooleanSupplier;

public final class GradleExecutionService {

    private static final Duration DEFAULT_TIMEOUT = Duration.ofMinutes(5);

    private final Path projectRoot;
    private final ProcessRunner processRunner;
    private final BooleanSupplier isWindows;
    private final Duration timeout;

    public GradleExecutionService(@Nonnull Path projectRoot) {
        this(projectRoot, new ProcessBuilderRunner(), GradleExecutionService::isWindowsHost, DEFAULT_TIMEOUT);
    }

    GradleExecutionService(
            @Nonnull Path projectRoot,
            @Nonnull ProcessRunner processRunner,
            @Nonnull BooleanSupplier isWindows,
            @Nonnull Duration timeout) {
        this.projectRoot = Objects.requireNonNull(projectRoot, "projectRoot").toAbsolutePath().normalize();
        this.processRunner = Objects.requireNonNull(processRunner, "processRunner");
        this.isWindows = Objects.requireNonNull(isWindows, "isWindows");
        this.timeout = Objects.requireNonNull(timeout, "timeout");
    }

    @Tool("Run a Gradle task in the project workspace.")
    @Nonnull
    public String runGradleTask(@Nonnull String task) {
        List<String> command = buildCommand(Objects.requireNonNull(task, "task"));
        try {
            ProcessResult result = processRunner.run(command, projectRoot, true, timeout);
            if (result.exitCode() != 0) {
                throw new IllegalStateException(
                        "Gradle task failed: " + task + System.lineSeparator() + result.output());
            }
            return result.output();
        } catch (TimeoutException e) {
            throw new IllegalStateException("Gradle task timed out: " + task, e);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to run Gradle task: " + task, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Gradle task interrupted: " + task, e);
        }
    }

    private List<String> buildCommand(String task) {
        String trimmedTask = task.trim();
        if (trimmedTask.isEmpty()) {
            throw new IllegalArgumentException("task must not be blank");
        }
        ArrayList<String> command = new ArrayList<>();
        command.add(isWindows.getAsBoolean() ? "gradlew.bat" : "./gradlew");
        command.addAll(Arrays.asList(trimmedTask.split("\\s+")));
        return List.copyOf(command);
    }

    private static boolean isWindowsHost() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }

    interface ProcessRunner {

        @Nonnull
        ProcessResult run(
                @Nonnull List<String> command,
                @Nonnull Path workingDirectory,
                boolean redirectErrorStream,
                @Nonnull Duration timeout)
                throws IOException, InterruptedException, TimeoutException;
    }

    record ProcessResult(int exitCode, @Nonnull String output) {

        ProcessResult {
            Objects.requireNonNull(output, "output");
        }
    }

    private static final class ProcessBuilderRunner implements ProcessRunner {

        @Override
        @Nonnull
        public ProcessResult run(
                @Nonnull List<String> command,
                @Nonnull Path workingDirectory,
                boolean redirectErrorStream,
                @Nonnull Duration timeout)
                throws IOException, InterruptedException, TimeoutException {
            ProcessBuilder processBuilder = new ProcessBuilder(Objects.requireNonNull(command, "command"));
            processBuilder.directory(Objects.requireNonNull(workingDirectory, "workingDirectory").toFile());
            processBuilder.redirectErrorStream(redirectErrorStream);
            Process process = processBuilder.start();
            String output = readAll(process.getInputStream());
            if (!process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS)) {
                process.destroyForcibly();
                throw new TimeoutException("Gradle process exceeded timeout: " + timeout);
            }
            return new ProcessResult(process.exitValue(), output);
        }

        @Nonnull
        private static String readAll(InputStream inputStream) throws IOException {
            try (InputStream stream = inputStream; ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                stream.transferTo(outputStream);
                return outputStream.toString(StandardCharsets.UTF_8);
            }
        }
    }
}

