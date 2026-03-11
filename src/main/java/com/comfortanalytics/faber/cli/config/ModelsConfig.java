package com.comfortanalytics.faber.cli.config;

import com.comfortanalytics.faber.annotation.Nonnull;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public record ModelsConfig(
        @Nonnull Map<String, ProviderConfig> tier1,
        @Nonnull Map<String, ProviderConfig> tier2,
        @Nonnull Map<String, ProviderConfig> tier3) {

    public ModelsConfig {
        tier1 = copyProviders(Objects.requireNonNullElseGet(tier1, Map::of));
        tier2 = copyProviders(Objects.requireNonNullElseGet(tier2, Map::of));
        tier3 = copyProviders(Objects.requireNonNullElseGet(tier3, Map::of));
    }

    @Nonnull
    private static Map<String, ProviderConfig> copyProviders(@Nonnull Map<String, ProviderConfig> providers) {
        LinkedHashMap<String, ProviderConfig> copy = new LinkedHashMap<>(providers.size());
        for (Map.Entry<String, ProviderConfig> entry : providers.entrySet()) {
            String alias = Objects.requireNonNull(entry.getKey(), "provider alias").trim();
            if (alias.isEmpty()) {
                throw new IllegalArgumentException("provider alias must not be blank");
            }
            copy.put(alias, Objects.requireNonNull(entry.getValue(), "provider config"));
        }
        return Collections.unmodifiableMap(copy);
    }
}
