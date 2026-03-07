package com.comfortanalytics.faber.agents;

import com.comfortanalytics.faber.annotation.Nonnull;
import com.comfortanalytics.faber.model.ModelProviderManager;
import com.comfortanalytics.faber.model.ModelTier;
import com.comfortanalytics.faber.routing.AgentRole;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

public final class JavaDeveloperAgent extends BaseAgent {

    private static final ModelTier DEFAULT_MODEL_TIER = ModelTier.TIER2_BALANCED;

    private final List<Object> tools;

    public JavaDeveloperAgent(@Nonnull ModelProviderManager modelProviderManager) {
        this(
                defaultWorkspaceRoot(),
                new ModelProviderManagerAgentExecutionEngine(modelProviderManager),
                List.of(),
                new WorkspaceMapService(defaultWorkspaceRoot()));
    }

    public JavaDeveloperAgent(
            @Nonnull Path workspaceRoot,
            @Nonnull AgentExecutionEngine executionEngine,
            @Nonnull List<Object> tools) {
        this(workspaceRoot, executionEngine, tools, new WorkspaceMapService(workspaceRoot));
    }

    public JavaDeveloperAgent(
            @Nonnull Path workspaceRoot,
            @Nonnull AgentExecutionEngine executionEngine,
            @Nonnull List<Object> tools,
            @Nonnull WorkspaceMapService workspaceMapService) {
        super(
                AgentRole.JAVA_DEVELOPER,
                DEFAULT_MODEL_TIER,
                executionEngine,
                new WorkspaceCodeMapLoader(workspaceRoot),
                workspaceMapService);
        this.tools = List.copyOf(Objects.requireNonNull(tools, "tools"));
    }

    @Override
    @Nonnull
    protected String instructions() {
        return "You are a Java developer agent. Focus on correctness, maintainable Java code, and practical implementation details.";
    }

    @Override
    protected boolean shouldInjectCodeMapIntoSystemMessage() {
        return true;
    }

    @Override
    protected boolean shouldInjectWorkspaceMapIntoSystemMessage() {
        return true;
    }

    @Override
    @Nonnull
    protected List<Object> tools() {
        return tools;
    }

    @Nonnull
    private static Path defaultWorkspaceRoot() {
        return Path.of("").toAbsolutePath().normalize();
    }
}
