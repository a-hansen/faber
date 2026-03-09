package com.comfortanalytics.faber.cli;

import com.comfortanalytics.faber.annotation.Nonnull;
import com.comfortanalytics.faber.cli.config.FaberConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public final class ConfigLoader {

    private final ObjectMapper objectMapper;

    public ConfigLoader() {
        this(new ObjectMapper(new YAMLFactory()).findAndRegisterModules());
    }

    ConfigLoader(@Nonnull ObjectMapper objectMapper) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
    }

    @Nonnull
    public FaberConfig load(@Nonnull Path configPath) {
        Path resolvedPath = Objects.requireNonNull(configPath, "configPath").toAbsolutePath().normalize();
        if (!Files.exists(resolvedPath)) {
            throw new IllegalArgumentException("Config file not found: " + resolvedPath);
        }
        try {
            FaberConfig config = objectMapper.readValue(resolvedPath.toFile(), FaberConfig.class);
            return Objects.requireNonNull(config, "objectMapper returned null config");
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load Faber config from: " + resolvedPath, e);
        }
    }
}

