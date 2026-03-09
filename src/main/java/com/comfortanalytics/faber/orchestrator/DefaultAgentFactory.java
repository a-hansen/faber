package com.comfortanalytics.faber.orchestrator;

import com.comfortanalytics.faber.agents.AgentExecutionEngine;
import com.comfortanalytics.faber.agents.BaseAgent;
import com.comfortanalytics.faber.agents.DynamicAgent;
import com.comfortanalytics.faber.annotation.Nonnull;
import com.comfortanalytics.faber.routing.RoutingDecision;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

public final class DefaultAgentFactory implements AgentFactory {

    private final AgentExecutionEngine executionEngine;
    private final Map<String, Object> toolRegistry;
    private final Map<String, Supplier<String>> contextRegistry;

    public DefaultAgentFactory(
            @Nonnull AgentExecutionEngine executionEngine,
            @Nonnull Map<String, Object> toolRegistry,
            @Nonnull Map<String, Supplier<String>> contextRegistry) {
        this.executionEngine = Objects.requireNonNull(executionEngine, "executionEngine");
        this.toolRegistry = Map.copyOf(Objects.requireNonNull(toolRegistry, "toolRegistry"));
        this.contextRegistry = Map.copyOf(Objects.requireNonNull(contextRegistry, "contextRegistry"));
    }

    @Override
    @Nonnull
    public BaseAgent create(@Nonnull RoutingDecision decision) {
        RoutingDecision nonNullDecision = Objects.requireNonNull(decision, "decision");
        return new DynamicAgent(
                nonNullDecision.role(),
                nonNullDecision.modelTier(),
                nonNullDecision.persona(),
                executionEngine,
                resolveTools(nonNullDecision.requiredTools()),
                resolveContext(nonNullDecision.requiredContexts()));
    }

    @Nonnull
    private List<Object> resolveTools(@Nonnull List<String> requiredTools) {
        List<String> nonNullRequiredTools = Objects.requireNonNull(requiredTools, "requiredTools");
        List<Object> resolvedTools = new ArrayList<>(nonNullRequiredTools.size());
        for (String toolId : nonNullRequiredTools) {
            Object tool = toolRegistry.get(toolId);
            if (tool == null) {
                throw new IllegalArgumentException("Unknown tool id: " + toolId);
            }
            resolvedTools.add(tool);
        }
        return List.copyOf(resolvedTools);
    }

    @Nonnull
    private String resolveContext(@Nonnull List<String> requiredContexts) {
        List<String> nonNullRequiredContexts = Objects.requireNonNull(requiredContexts, "requiredContexts");
        if (nonNullRequiredContexts.isEmpty()) {
            return "";
        }

        StringBuilder builder = new StringBuilder(256);
        for (String contextId : nonNullRequiredContexts) {
            Supplier<String> supplier = contextRegistry.get(contextId);
            if (supplier == null) {
                throw new IllegalArgumentException("Unknown context id: " + contextId);
            }
            String contextValue = Objects.requireNonNull(supplier.get(), "context supplier returned null for " + contextId);
            if (!builder.isEmpty()) {
                builder.append(System.lineSeparator()).append(System.lineSeparator());
            }
            builder.append(contextHeading(contextId))
                    .append(System.lineSeparator())
                    .append(contextValue);
        }
        return builder.toString();
    }

    @Nonnull
    private String contextHeading(@Nonnull String contextId) {
        return switch (Objects.requireNonNull(contextId, "contextId")) {
            case "code_map" -> "Current codebase architecture:";
            case "workspace_index" -> "Current workspace map:";
            default -> "Context " + contextId + ':';
        };
    }
}
