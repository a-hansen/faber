package com.comfortanalytics.faber.agents;

import com.comfortanalytics.faber.annotation.Nonnull;
import com.comfortanalytics.faber.model.ModelTier;
import java.util.List;
import java.util.Objects;

public record AgentExecutionRequest(
        @Nonnull String systemMessage,
        @Nonnull String userMessage,
        @Nonnull ModelTier modelTier,
        @Nonnull List<Object> tools) {

    public AgentExecutionRequest {
        Objects.requireNonNull(systemMessage, "systemMessage");
        Objects.requireNonNull(userMessage, "userMessage");
        Objects.requireNonNull(modelTier, "modelTier");
        tools = List.copyOf(Objects.requireNonNull(tools, "tools"));
    }
}

