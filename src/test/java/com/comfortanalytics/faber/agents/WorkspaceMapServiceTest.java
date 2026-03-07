package com.comfortanalytics.faber.agents;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class WorkspaceMapServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void indexesPackagesTypesAndPublicMethodsAcrossTheWorkspace() throws Exception {
        Path srcDir = tempDir.resolve("src/main/java/com/example");
        Files.createDirectories(srcDir);
        Files.writeString(srcDir.resolve("SampleService.java"), """
                package com.example;

                public final class SampleService {

                    public SampleService() {
                    }

                    public String greet(String name) {
                        return name;
                    }

                    private void hidden() {
                    }
                }
                """);
        Files.writeString(srcDir.resolve("SampleRecord.java"), """
                package com.example;

                public record SampleRecord(String name, int count) {

                    public String label() {
                        return name + count;
                    }
                }
                """);
        Path buildDir = tempDir.resolve("build/generated");
        Files.createDirectories(buildDir);
        Files.writeString(buildDir.resolve("Ignored.java"), "public class Ignored { public void skip() {} }");

        String workspaceMap = new WorkspaceMapService(tempDir).loadWorkspaceMap();

        assertTrue(workspaceMap.contains("Package com.example"));
        assertTrue(workspaceMap.contains("SampleService (class)"));
        assertTrue(workspaceMap.contains("public SampleService()"));
        assertTrue(workspaceMap.contains("public String greet(String name)"));
        assertFalse(workspaceMap.contains("hidden"));
        assertTrue(workspaceMap.contains("SampleRecord (record)"));
        assertTrue(workspaceMap.contains("public SampleRecord(String name, int count)"));
        assertTrue(workspaceMap.contains("public String name()"));
        assertTrue(workspaceMap.contains("public int count()"));
        assertFalse(workspaceMap.contains("Ignored"));
    }
}

