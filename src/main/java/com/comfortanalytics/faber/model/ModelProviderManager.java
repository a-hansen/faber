package com.comfortanalytics.faber.model;

import com.comfortanalytics.faber.annotation.Nonnull;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeoutException;

public final class ModelProviderManager {

    private static final int HTTP_TOO_MANY_REQUESTS = 429;

    private final Map<ModelTier, List<ModelProviderConfig>> tierConfigs;
    private final Map<String, ModelProvider> providers;

    public ModelProviderManager(
            @Nonnull Map<ModelTier, List<ModelProviderConfig>> tierConfigs,
            @Nonnull List<ModelProvider> providers) {
        this.tierConfigs = copyTierConfigs(Objects.requireNonNull(tierConfigs, "tierConfigs"));
        this.providers = indexProviders(Objects.requireNonNull(providers, "providers"));
    }

    @Nonnull
    public String generate(@Nonnull ModelTier tier, @Nonnull String prompt) throws TimeoutException {
        List<ModelProviderConfig> configs = tierConfigs.get(Objects.requireNonNull(tier, "tier"));
        Objects.requireNonNull(prompt, "prompt");
        if (configs == null || configs.isEmpty()) {
            throw new IllegalStateException("No model providers configured for tier: " + tier);
        }

        // Try providers in configured order until one succeeds or a non-retryable failure occurs.
        Throwable lastRetryableFailure = null;
        for (ModelProviderConfig config : configs) {
            ModelProvider provider = providers.get(config.providerId());
            if (provider == null) {
                throw new IllegalStateException("No provider registered with id: " + config.providerId());
            }
            try {
                return provider.generate(prompt);
            } catch (TimeoutException | RateLimitException e) {
                lastRetryableFailure = e;
            } catch (HttpException e) {
                if (!isRetryable(e)) {
                    throw e;
                }
                lastRetryableFailure = e;
            }
        }
        rethrowLastFailure(tier, lastRetryableFailure);
        throw new IllegalStateException("Unreachable fallback state for tier: " + tier);
    }

    private static Map<ModelTier, List<ModelProviderConfig>> copyTierConfigs(
            Map<ModelTier, List<ModelProviderConfig>> tierConfigs) {
        Map<ModelTier, List<ModelProviderConfig>> copy = new LinkedHashMap<>(tierConfigs.size());
        for (Map.Entry<ModelTier, List<ModelProviderConfig>> entry : tierConfigs.entrySet()) {
            List<ModelProviderConfig> configs = entry.getValue();
            copy.put(
                    Objects.requireNonNull(entry.getKey(), "tierConfigs contains a null tier"),
                    List.copyOf(Objects.requireNonNull(configs, "tierConfigs contains a null provider list")));
        }
        return Map.copyOf(copy);
    }

    private static Map<String, ModelProvider> indexProviders(List<ModelProvider> providers) {
        Map<String, ModelProvider> indexedProviders = new LinkedHashMap<>(providers.size());
        for (ModelProvider provider : providers) {
            ModelProvider nonNullProvider = Objects.requireNonNull(provider, "providers contains a null provider");
            ModelProvider previous = indexedProviders.put(nonNullProvider.providerId(), nonNullProvider);
            if (previous != null) {
                throw new IllegalArgumentException(
                        "Duplicate provider id: " + nonNullProvider.providerId());
            }
        }
        return Map.copyOf(indexedProviders);
    }

    private static boolean isRetryable(HttpException exception) {
        return exception.statusCode() == HTTP_TOO_MANY_REQUESTS;
    }

    private static void rethrowLastFailure(ModelTier tier, Throwable lastFailure) throws TimeoutException {
        if (lastFailure == null) {
            throw new IllegalStateException("No provider attempts were made for tier: " + tier);
        }
        if (lastFailure instanceof TimeoutException timeoutException) {
            throw timeoutException;
        }
        if (lastFailure instanceof RuntimeException runtimeException) {
            throw runtimeException;
        }
        throw new IllegalStateException("Unexpected provider failure type for tier: " + tier, lastFailure);
    }
}
