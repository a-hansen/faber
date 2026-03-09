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
    void indexesOnlyProductionSourcesAndFormatsCompactOutput() throws Exception {
        Path mainSrcDir = tempDir.resolve("src/main/java/com/example");
        Files.createDirectories(mainSrcDir);
        Files.writeString(mainSrcDir.resolve("SampleService.java"), """
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
        Files.writeString(mainSrcDir.resolve("SampleRecord.java"), """
                package com.example;

                public record SampleRecord(String name, int count) {

                    public String label() {
                        return name + count;
                    }
                }
                """);
        Path testSrcDir = tempDir.resolve("src/test/java/com/example");
        Files.createDirectories(testSrcDir);
        Files.writeString(testSrcDir.resolve("IgnoredTestType.java"), """
                package com.example;

                public final class IgnoredTestType {

                    public String skip() {
                        return "skip";
                    }
                }
                """);
        Path buildDir = tempDir.resolve("build/generated");
        Files.createDirectories(buildDir);
        Files.writeString(buildDir.resolve("Ignored.java"), "public class Ignored { public void skip() {} }");

        String workspaceMap = new WorkspaceMapService(tempDir).loadWorkspaceMap();

        assertTrue(workspaceMap.contains("pkg com.example"), workspaceMap);
        assertTrue(workspaceMap.contains("SampleService[class]: public SampleService(); public String greet(String name)"));
        assertFalse(workspaceMap.contains("hidden"));
        assertTrue(workspaceMap.contains("SampleRecord[record]: public SampleRecord(String name, int count); public String label(); public String name(); public int count()")
                || workspaceMap.contains("SampleRecord[record]: public SampleRecord(String name, int count); public int count(); public String label(); public String name()")
                || workspaceMap.contains("SampleRecord[record]: public SampleRecord(String name, int count); public int count(); public String name(); public String label()")
                || workspaceMap.contains("SampleRecord[record]: public SampleRecord(String name, int count); public String name(); public int count(); public String label()"));
        assertFalse(workspaceMap.contains("IgnoredTestType"));
        assertFalse(workspaceMap.contains("Ignored"));
    }

    @Test
    void indexesNestedPublicTypesAndRefreshesCacheWhenSourcesChange() throws Exception {
        Path srcDir = tempDir.resolve("src/main/java/com/example");
        Files.createDirectories(srcDir);
        Path sourceFile = srcDir.resolve("OuterType.java");
        Files.writeString(sourceFile, """
                package com.example;

                public final class OuterType {

                    public String outer() {
                        return "outer";
                    }

                    public static final class InnerType {

                        public int value() {
                            return 1;
                        }
                    }

                    public record InnerRecord(String name) {

                        public String label() {
                            return name;
                        }
                    }
                }
                """);
        WorkspaceMapService service = new WorkspaceMapService(tempDir);

        String initialMap = service.loadWorkspaceMap();

        assertTrue(initialMap.contains("OuterType[class]: public String outer()"), initialMap);
        assertTrue(initialMap.contains("OuterType.InnerType[class]: public int value()"), initialMap);
        assertTrue(initialMap.contains("OuterType.InnerRecord[record]"));
        assertTrue(initialMap.contains("public InnerRecord(String name)"));
        assertTrue(initialMap.contains("public String label()"));
        assertTrue(initialMap.contains("public String name()"));

        Files.writeString(sourceFile, """
                package com.example;

                public final class OuterType {

                    public String updated() {
                        return "updated";
                    }

                    public static final class InnerType {

                        public int changed() {
                            return 2;
                        }
                    }
                }
                """);
        Files.writeString(srcDir.resolve("AddedType.java"), """
                package com.example;

                public final class AddedType {

                    public boolean ready() {
                        return true;
                    }
                }
                """);

        String updatedMap = service.loadWorkspaceMap();

        assertTrue(updatedMap.contains("OuterType[class]: public String updated()"));
        assertTrue(updatedMap.contains("OuterType.InnerType[class]: public int changed()"));
        assertTrue(updatedMap.contains("AddedType[class]: public boolean ready()"));
        assertFalse(updatedMap.contains("public String outer()"));
        assertFalse(updatedMap.contains("InnerRecord"));

        Files.delete(sourceFile);

        String afterDeleteMap = service.loadWorkspaceMap();

        assertTrue(afterDeleteMap.contains("AddedType[class]: public boolean ready()"));
        assertFalse(afterDeleteMap.contains("OuterType"));
    }
}
