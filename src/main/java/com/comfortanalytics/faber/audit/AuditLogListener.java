package com.comfortanalytics.faber.audit;

import com.comfortanalytics.faber.annotation.Nonnull;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.listener.ChatModelErrorContext;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.listener.ChatModelResponseContext;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Clock;
import java.util.Objects;

public final class AuditLogListener implements ChatModelListener {

    private final Path transcriptFile;
    private final String agent;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public AuditLogListener(@Nonnull Path transcriptFile, @Nonnull String agent) {
        this(transcriptFile, agent, defaultObjectMapper(), Clock.systemUTC());
    }

    AuditLogListener(
            @Nonnull Path transcriptFile,
            @Nonnull String agent,
            @Nonnull ObjectMapper objectMapper,
            @Nonnull Clock clock) {
        this.transcriptFile = Objects.requireNonNull(transcriptFile, "transcriptFile").toAbsolutePath().normalize();
        this.agent = Objects.requireNonNull(agent, "agent");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Override
    public void onResponse(@Nonnull ChatModelResponseContext context) {
        ChatModelResponseContext nonNullContext = Objects.requireNonNull(context, "context");
        ChatTranscriptEntry entry = new ChatTranscriptEntry(
                clock.instant(),
                agent,
                promptFrom(nonNullContext.chatRequest().messages().toArray(ChatMessage[]::new)),
                responseFrom(nonNullContext.chatResponse().aiMessage()));
        append(entry);
    }

    @Override
    public void onError(@Nonnull ChatModelErrorContext context) {
        ChatModelErrorContext nonNullContext = Objects.requireNonNull(context, "context");
        ChatTranscriptEntry entry = new ChatTranscriptEntry(
                clock.instant(),
                agent,
                promptFrom(nonNullContext.chatRequest().messages().toArray(ChatMessage[]::new)),
                "ERROR: " + nonNullContext.error().getMessage());
        append(entry);
    }

    private void append(ChatTranscriptEntry entry) {
        try {
            Path parent = transcriptFile.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(
                    transcriptFile,
                    objectMapper.writeValueAsString(entry) + System.lineSeparator(),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to append transcript entry to: " + transcriptFile, e);
        }
    }

    private static ObjectMapper defaultObjectMapper() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Nonnull
    private static String promptFrom(ChatMessage[] messages) {
        StringBuilder builder = new StringBuilder(messages.length * 48);
        for (ChatMessage message : messages) {
            builder.append(messageText(message)).append(System.lineSeparator());
        }
        return builder.toString().stripTrailing();
    }

    @Nonnull
    private static String responseFrom(AiMessage message) {
        if (message == null) {
            return "";
        }
        String text = message.text();
        return text == null ? message.toString() : text;
    }

    @Nonnull
    private static String messageText(ChatMessage message) {
        if (message instanceof UserMessage userMessage) {
            String text = userMessage.hasSingleText() ? userMessage.singleText() : userMessage.contents().toString();
            return "USER: " + text;
        }
        if (message instanceof AiMessage aiMessage) {
            return "AI: " + responseFrom(aiMessage);
        }
        return message.type() + ": " + message;
    }
}

