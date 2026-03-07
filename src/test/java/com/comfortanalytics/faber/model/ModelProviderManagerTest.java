package com.comfortanalytics.faber.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.Test;

class ModelProviderManagerTest {

    @Test
    void returnsThePrimaryProviderResponseWhenItSucceeds() throws TimeoutException {
        StubModelProvider primary = StubModelProvider.returning("ollama", "primary-response");
        StubModelProvider fallback = StubModelProvider.returning("gemini", "fallback-response");
        ModelProviderManager manager = manager(primary, fallback);

        assertEquals("primary-response", manager.generate(ModelTier.TIER1_FAST, "hello"));
        assertEquals(1, primary.calls());
        assertEquals(0, fallback.calls());
    }

    @Test
    void fallsBackAfterARateLimitFailure() throws TimeoutException {
        StubModelProvider primary = StubModelProvider.failing("ollama", new RateLimitException("slow down"));
        StubModelProvider fallback = StubModelProvider.returning("gemini", "fallback-response");
        ModelProviderManager manager = manager(primary, fallback);

        assertEquals("fallback-response", manager.generate(ModelTier.TIER1_FAST, "hello"));
        assertEquals(1, primary.calls());
        assertEquals(1, fallback.calls());
    }

    @Test
    void fallsBackAfterAnHttp429Failure() throws TimeoutException {
        StubModelProvider primary = StubModelProvider.failing("ollama", new HttpException(429, "rate limited"));
        StubModelProvider fallback = StubModelProvider.returning("gemini", "fallback-response");
        ModelProviderManager manager = manager(primary, fallback);

        assertEquals("fallback-response", manager.generate(ModelTier.TIER1_FAST, "hello"));
        assertEquals(1, primary.calls());
        assertEquals(1, fallback.calls());
    }

    @Test
    void propagatesNonRetryableHttpFailuresWithoutTryingFallback() {
        StubModelProvider primary = StubModelProvider.failing("ollama", new HttpException(500, "server error"));
        StubModelProvider fallback = StubModelProvider.returning("gemini", "fallback-response");
        ModelProviderManager manager = manager(primary, fallback);

        HttpException exception = assertThrows(
                HttpException.class,
                () -> manager.generate(ModelTier.TIER1_FAST, "hello"));
        assertEquals(500, exception.statusCode());
        assertEquals(1, primary.calls());
        assertEquals(0, fallback.calls());
    }

    @Test
    void rethrowsTheLastRetryableFailureWhenAllProvidersFail() {
        StubModelProvider primary = StubModelProvider.failing("ollama", new RateLimitException("rate limit"));
        StubModelProvider fallback = StubModelProvider.failing("gemini", timeout("timed out"));
        ModelProviderManager manager = manager(primary, fallback);

        TimeoutException exception = assertThrows(
                TimeoutException.class,
                () -> manager.generate(ModelTier.TIER1_FAST, "hello"));
        assertEquals("timed out", exception.getMessage());
        assertEquals(1, primary.calls());
        assertEquals(1, fallback.calls());
    }

    @Test
    void rejectsMissingTierConfiguration() {
        ModelProviderManager manager = new ModelProviderManager(Map.of(), List.of());

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> manager.generate(ModelTier.TIER1_FAST, "hello"));
        assertEquals("No model providers configured for tier: TIER1_FAST", exception.getMessage());
    }

    private static ModelProviderManager manager(ModelProvider... providers) {
        return new ModelProviderManager(
                Map.of(
                        ModelTier.TIER1_FAST,
                        List.of(
                                new ModelProviderConfig("ollama", "llama3"),
                                new ModelProviderConfig("gemini", "gemini-pro"))),
                List.of(providers));
    }

    private static TimeoutException timeout(String message) {
        return new TimeoutException(message);
    }

    private static final class StubModelProvider implements ModelProvider {

        private final String providerId;
        private final String response;
        private final RuntimeException runtimeFailure;
        private final TimeoutException timeoutFailure;
        private int calls;

        private StubModelProvider(
                String providerId,
                String response,
                RuntimeException runtimeFailure,
                TimeoutException timeoutFailure) {
            this.providerId = providerId;
            this.response = response;
            this.runtimeFailure = runtimeFailure;
            this.timeoutFailure = timeoutFailure;
        }

        static StubModelProvider returning(String providerId, String response) {
            return new StubModelProvider(providerId, response, null, null);
        }

        static StubModelProvider failing(String providerId, RuntimeException failure) {
            return new StubModelProvider(providerId, null, failure, null);
        }

        static StubModelProvider failing(String providerId, TimeoutException failure) {
            return new StubModelProvider(providerId, null, null, failure);
        }

        @Override
        public String providerId() {
            return providerId;
        }

        @Override
        public String generate(String prompt) throws TimeoutException {
            calls++;
            if (runtimeFailure != null) {
                throw runtimeFailure;
            }
            if (timeoutFailure != null) {
                throw timeoutFailure;
            }
            return response;
        }

        int calls() {
            return calls;
        }
    }
}

