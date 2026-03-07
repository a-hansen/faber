package com.comfortanalytics.faber.routing;

import com.comfortanalytics.faber.annotation.Nonnull;
import com.comfortanalytics.faber.orchestrator.TaskRequest;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public final class RuleBasedRoutingStrategy implements RoutingStrategy {

    private static final List<String> JAVA_KEYWORDS = List.of(
            "java",
            "gradle",
            "junit",
            "compile",
            "refactor",
            "method",
            "class",
            "code",
            "bug");
    private static final List<String> FINANCE_KEYWORDS = List.of(
            "stock",
            "stocks",
            "market",
            "portfolio",
            "investment",
            "investing",
            "finance",
            "financial",
            "earnings");
    private static final AgentRole DEFAULT_ROLE = AgentRole.JAVA_DEVELOPER;

    @Override
    @Nonnull
    public AgentRole route(@Nonnull TaskRequest request) {
        String input = Objects.requireNonNull(request, "request").userInput().toLowerCase(Locale.ROOT);

        // Match software-development requests first.
        if (containsAnyKeyword(input, JAVA_KEYWORDS)) {
            return AgentRole.JAVA_DEVELOPER;
        }

        // Match finance-oriented requests next.
        if (containsAnyKeyword(input, FINANCE_KEYWORDS)) {
            return AgentRole.FINANCIAL_ANALYST;
        }
        return DEFAULT_ROLE;
    }

    private boolean containsAnyKeyword(String input, List<String> keywords) {
        for (String keyword : keywords) {
            if (input.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}

