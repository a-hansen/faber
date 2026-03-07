package com.comfortanalytics.faber.agents;

import com.comfortanalytics.faber.annotation.Nonnull;
import com.comfortanalytics.faber.model.ModelTier;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import java.util.Objects;
import java.util.function.Function;
import java.util.concurrent.TimeoutException;

public final class LangChain4jAgentExecutionEngine implements AgentExecutionEngine {

    private final Function<ModelTier, ChatModel> chatModelResolver;

    public LangChain4jAgentExecutionEngine(@Nonnull Function<ModelTier, ChatModel> chatModelResolver) {
        this.chatModelResolver = Objects.requireNonNull(chatModelResolver, "chatModelResolver");
    }

    @Override
    @Nonnull
    public String execute(@Nonnull AgentExecutionRequest request) throws TimeoutException {
        AgentExecutionRequest nonNullRequest = Objects.requireNonNull(request, "request");

        // Build an AI service per invocation so the system message and injected tools reflect the current workspace state.
        ChatModel chatModel = Objects.requireNonNull(
                chatModelResolver.apply(nonNullRequest.modelTier()),
                "chatModelResolver returned null");
        ToolAwareAgentService agentService = AiServices.builder(ToolAwareAgentService.class)
                .chatModel(chatModel)
                .systemMessage(nonNullRequest.systemMessage())
                .userMessageProvider(input -> ((UserInput) input).message())
                .tools(nonNullRequest.tools())
                .build();
        return agentService.execute(new UserInput(nonNullRequest.userMessage()));
    }

    interface ToolAwareAgentService {

        @Nonnull
        String execute(@Nonnull UserInput input);
    }

    record UserInput(@Nonnull String message) {

        UserInput {
            Objects.requireNonNull(message, "message");
        }
    }
}

