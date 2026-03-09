package com.comfortanalytics.faber.agents;

import com.comfortanalytics.faber.annotation.Nonnull;
import com.comfortanalytics.faber.model.ModelTier;
import com.comfortanalytics.faber.routing.AgentRole;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

public final class DynamicAgent extends BaseAgent {

    private static final Path DEFAULT_WORKSPACE_ROOT = Path.of("").toAbsolutePath().normalize();

    private final String persona;
    private final List<Object> tools;
    private final String injectedContext;

    public DynamicAgent(
            @Nonnull AgentRole role,
            @Nonnull ModelTier modelTier,
            @Nonnull String persona,
            @Nonnull AgentExecutionEngine executionEngine,
            @Nonnull List<Object> tools,
            @Nonnull String injectedContext) {
        super(
                role,
                modelTier,
                executionEngine,
                new WorkspaceCodeMapLoader(DEFAULT_WORKSPACE_ROOT),
                new WorkspaceMapService(DEFAULT_WORKSPACE_ROOT));
        this.persona = Objects.requireNonNull(persona, "persona");
        this.tools = List.copyOf(Objects.requireNonNull(tools, "tools"));
        this.injectedContext = Objects.requireNonNull(injectedContext, "injectedContext");
    }

    @Override
    @Nonnull
    protected String instructions() {
        return persona;
    }

    @Override
    @Nonnull
    protected List<Object> tools() {
        return tools;
    }

    @Override
    @Nonnull
    protected String injectedContext() {
        return injectedContext;
    }
}

