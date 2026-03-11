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
        Path configPath = tempDir.resolve("config.yml");
        Files.writeString(configPath, """
                workspace:
                  rootPath: workspace
                routing:
                  mode: dynamic
                models:
                  tier1:
                    primary:
                      provider: gemini
                      model: gemini-2.1-flash-lite-preview
                      apiKey: API-KEY
                  tier2:
                    primary:
                      provider: gemini
                      model: gemini-3-flash-preview
                      apiKey: API-KEY
                  tier3:
                    architect:
                      provider: gemini
                      model: gemini-3.1-pro-preview
                      apiKey: API-KEY
                """);

        FaberConfig config = new ConfigLoader().load(configPath);

        assertNotNull(config);
        assertEquals("workspace", config.workspace().rootPath());
        assertEquals("DYNAMIC", config.routing().mode());
        assertEquals("gemini", config.models().tier1().get("primary").provider());
        assertEquals("gemini-2.1-flash-lite-preview", config.models().tier1().get("primary").model());
        assertEquals("API-KEY", config.models().tier1().get("primary").apiKey());
        assertEquals("gemini", config.models().tier2().get("primary").provider());
        assertEquals("gemini-3-flash-preview", config.models().tier2().get("primary").model());
        assertEquals("gemini", config.models().tier3().get("architect").provider());
        assertEquals("gemini-3.1-pro-preview", config.models().tier3().get("architect").model());
        assertEquals("API-KEY", config.models().tier3().get("architect").apiKey());
    }
}
