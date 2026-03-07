package com.comfortanalytics.faber.agents;

import com.comfortanalytics.faber.annotation.Nonnull;
import com.comfortanalytics.faber.model.ModelProviderManager;
import java.util.Objects;
import java.util.concurrent.TimeoutException;

final class ModelProviderManagerAgentExecutionEngine implements AgentExecutionEngine {

    private final ModelProviderManager modelProviderManager;

    ModelProviderManagerAgentExecutionEngine(@Nonnull ModelProviderManager modelProviderManager) {
        this.modelProviderManager = Objects.requireNonNull(modelProviderManager, "modelProviderManager");
    }

    @Override
    @Nonnull
    public String execute(@Nonnull AgentExecutionRequest request) throws TimeoutException {
        AgentExecutionRequest nonNullRequest = Objects.requireNonNull(request, "request");
        String prompt = nonNullRequest.systemMessage()
                + System.lineSeparator()
                + "User request: "
                + nonNullRequest.userMessage();
        return modelProviderManager.generate(nonNullRequest.modelTier(), prompt);
    }
}

