package com.comfortanalytics.faber.agents;

import com.comfortanalytics.faber.annotation.Nonnull;
import java.util.concurrent.TimeoutException;

@FunctionalInterface
public interface AgentExecutionEngine {

    @Nonnull
    String execute(@Nonnull AgentExecutionRequest request) throws TimeoutException;
}

