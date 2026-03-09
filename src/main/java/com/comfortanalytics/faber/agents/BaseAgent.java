package com.comfortanalytics.faber.agents;

import com.comfortanalytics.faber.annotation.Nonnull;
import com.comfortanalytics.faber.model.ModelTier;
import com.comfortanalytics.faber.orchestrator.TaskRequest;
import com.comfortanalytics.faber.routing.AgentRole;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeoutException;

public abstract class BaseAgent {

    private final AgentRole role;
    private final ModelTier modelTier;
    private final AgentExecutionEngine executionEngine;
    private final WorkspaceCodeMapLoader codeMapLoader;
    private final WorkspaceMapService workspaceMapService;

    protected BaseAgent(
            @Nonnull AgentRole role,
            @Nonnull ModelTier modelTier,
            @Nonnull AgentExecutionEngine executionEngine,
            @Nonnull WorkspaceCodeMapLoader codeMapLoader,
            @Nonnull WorkspaceMapService workspaceMapService) {
        this.role = Objects.requireNonNull(role, "role");
        this.modelTier = Objects.requireNonNull(modelTier, "modelTier");
        this.executionEngine = Objects.requireNonNull(executionEngine, "executionEngine");
        this.codeMapLoader = Objects.requireNonNull(codeMapLoader, "codeMapLoader");
        this.workspaceMapService = Objects.requireNonNull(workspaceMapService, "workspaceMapService");
    }

    @Nonnull
    public final AgentRole role() {
        return role;
    }

    @Nonnull
    public final ModelTier modelTier() {
        return modelTier;
    }

    @Nonnull
    public final String handle(@Nonnull TaskRequest request) throws TimeoutException {
        return handle(request, modelTier);
    }

    @Nonnull
    public final String handle(@Nonnull TaskRequest request, @Nonnull ModelTier requestedTier) throws TimeoutException {
        TaskRequest nonNullRequest = Objects.requireNonNull(request, "request");
        ModelTier nonNullTier = Objects.requireNonNull(requestedTier, "requestedTier");
        AgentExecutionRequest executionRequest = new AgentExecutionRequest(
                buildSystemMessage(),
                buildUserMessage(nonNullRequest),
                nonNullTier,
                tools());
        return executionEngine.execute(executionRequest);
    }

    @Nonnull
    protected abstract String instructions();

    protected boolean shouldInjectCodeMapIntoSystemMessage() {
        return false;
    }

    protected boolean shouldInjectWorkspaceMapIntoSystemMessage() {
        return false;
    }

    @Nonnull
    protected List<Object> tools() {
        return List.of();
    }

    @Nonnull
    protected String injectedContext() {
        return "";
    }

    @Nonnull
    protected String buildUserMessage(@Nonnull TaskRequest request) {
        return Objects.requireNonNull(request, "request").userInput();
    }

    @Nonnull
    private String buildSystemMessage() {
        StringBuilder builder = new StringBuilder(512);

        // Describe the agent role and its operating instructions.
        builder.append("Agent role: ")
                .append(role)
                .append(System.lineSeparator())
                .append("Instructions: ")
                .append(instructions());

        // Inject the curated code map when the agent needs architecture context before tool use.
        if (shouldInjectCodeMapIntoSystemMessage()) {
            builder.append(System.lineSeparator())
                    .append(System.lineSeparator())
                    .append("Current codebase architecture:")
                    .append(System.lineSeparator())
                    .append(codeMapLoader.loadCodeMap());
        }

        // Inject the current runtime workspace map so developer agents have fresh class and method context.
        if (shouldInjectWorkspaceMapIntoSystemMessage()) {
            builder.append(System.lineSeparator())
                    .append(System.lineSeparator())
                    .append("Current workspace map:")
                    .append(System.lineSeparator())
                    .append(workspaceMapService.loadWorkspaceMap());
        }

        // Append factory-selected context sections for dynamically composed agents.
        String extraContext = injectedContext();
        if (!extraContext.isBlank()) {
            builder.append(System.lineSeparator())
                    .append(System.lineSeparator())
                    .append("Injected context:")
                    .append(System.lineSeparator())
                    .append(extraContext);
        }
        return builder.toString();
    }
}
