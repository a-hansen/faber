package com.comfortanalytics.faber.agents;
import com.comfortanalytics.faber.model.ModelProvider;
import com.comfortanalytics.faber.model.ModelProviderConfig;
import com.comfortanalytics.faber.model.ModelProviderManager;
import com.comfortanalytics.faber.model.ModelTier;
import java.util.List;
import java.util.Map;
final class AgentTestSupport {
    private AgentTestSupport() {
    }
    static ModelProviderManager managerForTier(ModelTier tier, CapturingModelProvider provider) {
        return new ModelProviderManager(
                Map.of(tier, List.of(new ModelProviderConfig(provider.providerId(), provider.modelName()))),
                List.of(provider));
    }
    static final class CapturingModelProvider implements ModelProvider {
        private final String providerId;
        private final String modelName;
        private final String response;
        private String lastPrompt;
        private int calls;
        CapturingModelProvider(String providerId, String modelName, String response) {
            this.providerId = providerId;
            this.modelName = modelName;
            this.response = response;
        }
        @Override
        public String providerId() {
            return providerId;
        }
        String modelName() {
            return modelName;
        }
        @Override
        public String generate(String prompt) {
            calls++;
            lastPrompt = prompt;
            return response;
        }
        String lastPrompt() {
            return lastPrompt;
        }
        int calls() {
            return calls;
        }
    }
}
