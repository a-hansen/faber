package com.comfortanalytics.faber.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SandboxedFileServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void writesAndReadsFilesInsideTheSandbox() throws IOException {
        SandboxedFileService service = new SandboxedFileService(tempDir, SandboxedFileService.Mode.READ_WRITE);

        assertEquals("Wrote file: note.txt", service.writeFile("notes/note.txt", "hello"));
        assertEquals("hello", service.readFile("notes/note.txt"));
        assertEquals("hello", Files.readString(tempDir.resolve("notes").resolve("note.txt")));
    }

    @Test
    void blocksWritesInReadOnlyMode() {
        SandboxedFileService service = new SandboxedFileService(tempDir, SandboxedFileService.Mode.READ_ONLY);

        assertThrows(UnsupportedOperationException.class, () -> service.writeFile("note.txt", "hello"));
    }

    @Test
    void blocksPathTraversal() {
        SandboxedFileService service = new SandboxedFileService(tempDir, SandboxedFileService.Mode.READ_WRITE);

        assertThrows(SecurityException.class, () -> service.readFile("..\\outside.txt"));
        assertThrows(SecurityException.class, () -> service.writeFile("../outside.txt", "hello"));
    }

    @Test
    void allowsNormalizedNestedPathsThatRemainInsideTheSandbox() {
        SandboxedFileService service = new SandboxedFileService(tempDir, SandboxedFileService.Mode.READ_WRITE);

        service.writeFile("nested/child/../note.txt", "hello");

        assertEquals("hello", service.readFile("nested/note.txt"));
    }
}

