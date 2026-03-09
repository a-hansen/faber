package com.comfortanalytics.faber.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.comfortanalytics.faber.cli.config.FaberConfig;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ConfigLoaderTest {

    @TempDir
    Path tempDir;

    @Test
    void loadsYamlIntoTheExpectedConfigurationRecords() throws Exception {
        Path configPath = tempDir.resolve("faber.yml");
        Files.writeString(configPath, """
                workspace:
                  rootPath: workspace
                routing:
                  mode: dynamic
                models:
                  tier1:
                    primary:
                      provider: gemini
                      model: gemini-2.0-flash
                  tier2:
                    primary:
                      provider: openai
                      model: gpt-4.1-mini
                """);

        FaberConfig config = new ConfigLoader().load(configPath);

        assertNotNull(config);
        assertEquals("workspace", config.workspace().rootPath());
        assertEquals("DYNAMIC", config.routing().mode());
        assertEquals("gemini", config.models().tier1().get("primary").provider());
        assertEquals("gemini-2.0-flash", config.models().tier1().get("primary").model());
        assertEquals("openai", config.models().tier2().get("primary").provider());
        assertEquals("gpt-4.1-mini", config.models().tier2().get("primary").model());
    }
}

