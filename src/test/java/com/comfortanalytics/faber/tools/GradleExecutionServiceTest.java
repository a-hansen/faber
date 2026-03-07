package com.comfortanalytics.faber.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GradleExecutionServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void usesTheWindowsWrapperAndProjectDirectory() {
        CapturingRunner runner = new CapturingRunner(new GradleExecutionService.ProcessResult(0, "ok"));
        GradleExecutionService service = new GradleExecutionService(
                tempDir,
                runner,
                () -> true,
                Duration.ofMinutes(5));

        assertEquals("ok", service.runGradleTask("test"));
        assertEquals(List.of("gradlew.bat", "test"), runner.command());
        assertEquals(tempDir.toAbsolutePath().normalize(), runner.workingDirectory());
        assertTrue(runner.redirectErrorStream());
        assertEquals(Duration.ofMinutes(5), runner.timeout());
    }

    @Test
    void usesTheUnixWrapperForNonWindowsHosts() {
        CapturingRunner runner = new CapturingRunner(new GradleExecutionService.ProcessResult(0, "ok"));
        GradleExecutionService service = new GradleExecutionService(
                tempDir,
                runner,
                () -> false,
                Duration.ofSeconds(30));

        service.runGradleTask("clean test");

        assertEquals(List.of("./gradlew", "clean", "test"), runner.command());
    }

    @Test
    void rejectsBlankTasks() {
        CapturingRunner runner = new CapturingRunner(new GradleExecutionService.ProcessResult(0, "ok"));
        GradleExecutionService service = new GradleExecutionService(
                tempDir,
                runner,
                () -> true,
                Duration.ofMinutes(5));

        assertThrows(IllegalArgumentException.class, () -> service.runGradleTask("   "));
    }

    @Test
    void includesOutputWhenTheGradleTaskFails() {
        CapturingRunner runner = new CapturingRunner(new GradleExecutionService.ProcessResult(1, "test failed"));
        GradleExecutionService service = new GradleExecutionService(
                tempDir,
                runner,
                () -> true,
                Duration.ofMinutes(5));

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> service.runGradleTask("test"));

        assertTrue(exception.getMessage().contains("Gradle task failed: test"));
        assertTrue(exception.getMessage().contains("test failed"));
    }

    @Test
    void wrapsTimeoutsAsIllegalStateExceptions() {
        TimeoutRunner runner = new TimeoutRunner();
        GradleExecutionService service = new GradleExecutionService(
                tempDir,
                runner,
                () -> true,
                Duration.ofMinutes(5));

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> service.runGradleTask("test"));

        assertTrue(exception.getMessage().contains("Gradle task timed out: test"));
    }

    private static final class CapturingRunner implements GradleExecutionService.ProcessRunner {

        private final GradleExecutionService.ProcessResult result;
        private List<String> command = List.of();
        private Path workingDirectory;
        private boolean redirectErrorStream;
        private Duration timeout;

        private CapturingRunner(GradleExecutionService.ProcessResult result) {
            this.result = result;
        }

        @Override
        public GradleExecutionService.ProcessResult run(
                List<String> command,
                Path workingDirectory,
                boolean redirectErrorStream,
                Duration timeout) {
            this.command = List.copyOf(command);
            this.workingDirectory = workingDirectory;
            this.redirectErrorStream = redirectErrorStream;
            this.timeout = timeout;
            return result;
        }

        private List<String> command() {
            return command;
        }

        private Path workingDirectory() {
            return workingDirectory;
        }

        private boolean redirectErrorStream() {
            return redirectErrorStream;
        }

        private Duration timeout() {
            return timeout;
        }
    }

    private static final class TimeoutRunner implements GradleExecutionService.ProcessRunner {

        @Override
        public GradleExecutionService.ProcessResult run(
                List<String> command,
                Path workingDirectory,
                boolean redirectErrorStream,
                Duration timeout) throws TimeoutException {
            throw new TimeoutException("timed out");
        }
    }
}

