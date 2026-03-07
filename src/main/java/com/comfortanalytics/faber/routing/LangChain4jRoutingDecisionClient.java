package com.comfortanalytics.faber.routing;

import com.comfortanalytics.faber.annotation.Nonnull;
import com.comfortanalytics.faber.orchestrator.TaskRequest;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import java.util.Objects;

public final class LangChain4jRoutingDecisionClient implements RoutingDecisionClient {

    private static final String SYSTEM_MESSAGE = """
            You are a routing classifier for Project Faber.
            Return a structured RoutingDecision with:
            - role: one of JAVA_DEVELOPER, FINANCIAL_ANALYST, CONTEXT_CONDENSER
            - modelTier: one of TIER1_FAST, TIER2_BALANCED, TIER3_POWERFUL

            Choose the role that best matches the user's intent.
            Choose the model tier based on task complexity:
            - TIER1_FAST for simple classification or lightweight tasks
            - TIER2_BALANCED for most normal development or analysis tasks
            - TIER3_POWERFUL for complex, ambiguous, or high-reasoning tasks
            """;

    private final StructuredRoutingService routingService;

    public LangChain4jRoutingDecisionClient(@Nonnull ChatModel chatModel) {
        this(AiServices.builder(StructuredRoutingService.class)
                .chatModel(Objects.requireNonNull(chatModel, "chatModel"))
                .systemMessage(SYSTEM_MESSAGE)
                .userMessageProvider(input -> "User request:\n" + input)
                .build());
    }

    LangChain4jRoutingDecisionClient(@Nonnull StructuredRoutingService routingService) {
        this.routingService = Objects.requireNonNull(routingService, "routingService");
    }

    @Override
    @Nonnull
    public RoutingDecision routeDecision(@Nonnull TaskRequest request) {
        TaskRequest nonNullRequest = Objects.requireNonNull(request, "request");
        return Objects.requireNonNull(routingService.route(nonNullRequest.userInput()), "routingService returned null");
    }

    interface StructuredRoutingService {

        @Nonnull
        RoutingDecision route(@Nonnull String userInput);
    }
}

