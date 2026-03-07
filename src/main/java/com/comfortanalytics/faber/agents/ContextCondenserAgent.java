package com.comfortanalytics.faber.agents;

import com.comfortanalytics.faber.annotation.Nonnull;
import com.comfortanalytics.faber.memory.MemoryCondenser;
import com.comfortanalytics.faber.memory.MemoryMessage;
import com.comfortanalytics.faber.model.ModelProviderManager;
import com.comfortanalytics.faber.model.ModelTier;
import com.comfortanalytics.faber.orchestrator.TaskRequest;
import com.comfortanalytics.faber.routing.AgentRole;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeoutException;

public final class ContextCondenserAgent extends BaseAgent implements MemoryCondenser {

    private static final ModelTier DEFAULT_MODEL_TIER = ModelTier.TIER2_BALANCED;
    private static final String SUMMARY_ROLE = "assistant";

    public ContextCondenserAgent(@Nonnull ModelProviderManager modelProviderManager) {
        this(defaultWorkspaceRoot(), new ModelProviderManagerAgentExecutionEngine(modelProviderManager));
    }

    public ContextCondenserAgent(@Nonnull Path workspaceRoot, @Nonnull AgentExecutionEngine executionEngine) {
        super(
                AgentRole.CONTEXT_CONDENSER,
                DEFAULT_MODEL_TIER,
                executionEngine,
                new WorkspaceCodeMapLoader(workspaceRoot),
                new WorkspaceMapService(workspaceRoot));
    }

    @Override
    @Nonnull
    protected String instructions() {
        return "You are a context condenser agent. Produce a concise memory summary that preserves key facts, decisions, and open questions.";
    }

    @Override
    @Nonnull
    public MemoryMessage condense(@Nonnull List<MemoryMessage> messages) throws TimeoutException {
        List<MemoryMessage> nonNullMessages = List.copyOf(Objects.requireNonNull(messages, "messages"));
        if (nonNullMessages.isEmpty()) {
            throw new IllegalArgumentException("messages must not be empty");
        }

        // Turn the message history into a synthetic request for the shared agent pipeline.
        TaskRequest request = new TaskRequest(
                "memory-condense-" + nonNullMessages.size(),
                formatMessages(nonNullMessages),
                Map.of("purpose", "memory-condense"),
                Instant.now());
        return new MemoryMessage(SUMMARY_ROLE, handle(request));
    }

    private String formatMessages(List<MemoryMessage> messages) {
        StringBuilder builder = new StringBuilder(messages.size() * 32);
        for (MemoryMessage message : messages) {
            builder.append(message.role())
                    .append(": ")
                    .append(message.text())
                    .append('\n');
        }
        return builder.toString().stripTrailing();
    }

    @Nonnull
    private static Path defaultWorkspaceRoot() {
        return Path.of("").toAbsolutePath().normalize();
    }
}
