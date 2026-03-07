package com.comfortanalytics.faber.orchestrator;

import com.comfortanalytics.faber.agents.AgentExecutionEngine;
import com.comfortanalytics.faber.agents.BaseAgent;
import com.comfortanalytics.faber.agents.ContextCondenserAgent;
import com.comfortanalytics.faber.agents.FinancialAnalystAgent;
import com.comfortanalytics.faber.agents.JavaDeveloperAgent;
import com.comfortanalytics.faber.agents.WorkspaceMapService;
import com.comfortanalytics.faber.annotation.Nonnull;
import com.comfortanalytics.faber.routing.AgentRole;
import com.comfortanalytics.faber.tools.GradleExecutionService;
import com.comfortanalytics.faber.tools.SandboxedFileService;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

public final class DefaultAgentFactory implements AgentFactory {

    private final Path workspaceRoot;
    private final AgentExecutionEngine executionEngine;
    private final SandboxedFileService sandboxedFileService;
    private final GradleExecutionService gradleExecutionService;
    private final WorkspaceMapService workspaceMapService;

    public DefaultAgentFactory(
            @Nonnull Path workspaceRoot,
            @Nonnull AgentExecutionEngine executionEngine,
            @Nonnull SandboxedFileService sandboxedFileService,
            @Nonnull GradleExecutionService gradleExecutionService) {
        this.workspaceRoot = Objects.requireNonNull(workspaceRoot, "workspaceRoot").toAbsolutePath().normalize();
        this.executionEngine = Objects.requireNonNull(executionEngine, "executionEngine");
        this.sandboxedFileService = Objects.requireNonNull(sandboxedFileService, "sandboxedFileService");
        this.gradleExecutionService = Objects.requireNonNull(gradleExecutionService, "gradleExecutionService");
        this.workspaceMapService = new WorkspaceMapService(this.workspaceRoot);
    }

    @Override
    @Nonnull
    public BaseAgent create(@Nonnull AgentRole role) {
        AgentRole nonNullRole = Objects.requireNonNull(role, "role");
        return switch (nonNullRole) {
            case JAVA_DEVELOPER -> new JavaDeveloperAgent(
                    workspaceRoot,
                    executionEngine,
                    List.of(sandboxedFileService, gradleExecutionService),
                    workspaceMapService);
            case FINANCIAL_ANALYST -> new FinancialAnalystAgent(workspaceRoot, executionEngine);
            case CONTEXT_CONDENSER -> new ContextCondenserAgent(workspaceRoot, executionEngine);
        };
    }
}
