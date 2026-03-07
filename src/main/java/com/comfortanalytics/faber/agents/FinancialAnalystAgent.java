package com.comfortanalytics.faber.agents;

import com.comfortanalytics.faber.annotation.Nonnull;
import com.comfortanalytics.faber.model.ModelProviderManager;
import com.comfortanalytics.faber.model.ModelTier;
import com.comfortanalytics.faber.routing.AgentRole;
import java.nio.file.Path;

public final class FinancialAnalystAgent extends BaseAgent {

    private static final ModelTier DEFAULT_MODEL_TIER = ModelTier.TIER2_BALANCED;

    public FinancialAnalystAgent(@Nonnull ModelProviderManager modelProviderManager) {
        this(defaultWorkspaceRoot(), new ModelProviderManagerAgentExecutionEngine(modelProviderManager));
    }

    public FinancialAnalystAgent(@Nonnull Path workspaceRoot, @Nonnull AgentExecutionEngine executionEngine) {
        super(
                AgentRole.FINANCIAL_ANALYST,
                DEFAULT_MODEL_TIER,
                executionEngine,
                new WorkspaceCodeMapLoader(workspaceRoot),
                new WorkspaceMapService(workspaceRoot));
    }

    @Override
    @Nonnull
    protected String instructions() {
        return "You are a financial analyst agent. Focus on risk, market context, and clear financial reasoning.";
    }

    @Nonnull
    private static Path defaultWorkspaceRoot() {
        return Path.of("").toAbsolutePath().normalize();
    }
}
