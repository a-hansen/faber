package com.comfortanalytics.faber.audit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.ModelProvider;
import dev.langchain4j.model.chat.listener.ChatModelErrorContext;
import dev.langchain4j.model.chat.listener.ChatModelResponseContext;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AuditLogListenerTest {

    @TempDir
    Path tempDir;

    @Test
    void writesAJsonlTranscriptEntryForResponses() throws Exception {
        Path transcriptFile = tempDir.resolve("transcripts").resolve("Faber_JavaDeveloper_Transcript.jsonl");
        AuditLogListener listener = new AuditLogListener(
                transcriptFile,
                "JavaDeveloperAgent",
                objectMapper(),
                Clock.fixed(Instant.parse("2026-03-07T01:02:03Z"), ZoneOffset.UTC));
        ChatRequest request = ChatRequest.builder()
                .messages(UserMessage.from("Write a parser"))
                .build();
        ChatResponse response = ChatResponse.builder()
                .aiMessage(AiMessage.from("Here is a parser"))
                .build();

        listener.onResponse(new ChatModelResponseContext(response, request, ModelProvider.OPEN_AI, Map.of()));

        List<String> lines = Files.readAllLines(transcriptFile);
        assertEquals(1, lines.size());
        assertTrue(lines.get(0).contains("\"timestamp\":\"2026-03-07T01:02:03Z\""));
        assertTrue(lines.get(0).contains("\"agent\":\"JavaDeveloperAgent\""));
        assertTrue(lines.get(0).contains("\"prompt\":\"USER: Write a parser\""));
        assertTrue(lines.get(0).contains("\"response\":\"Here is a parser\""));
    }

    @Test
    void appendsErrorEntriesAsJsonlLines() throws Exception {
        Path transcriptFile = tempDir.resolve("transcripts").resolve("Faber_JavaDeveloper_Transcript.jsonl");
        AuditLogListener listener = new AuditLogListener(
                transcriptFile,
                "JavaDeveloperAgent",
                objectMapper(),
                Clock.fixed(Instant.parse("2026-03-07T01:02:03Z"), ZoneOffset.UTC));
        ChatRequest request = ChatRequest.builder()
                .messages(UserMessage.from("Run the build"))
                .build();

        listener.onError(new ChatModelErrorContext(
                new IllegalStateException("build failed"),
                request,
                ModelProvider.OPEN_AI,
                Map.of()));

        List<String> lines = Files.readAllLines(transcriptFile);
        assertEquals(1, lines.size());
        assertTrue(lines.get(0).contains("\"prompt\":\"USER: Run the build\""));
        assertTrue(lines.get(0).contains("\"response\":\"ERROR: build failed\""));
    }

    private static ObjectMapper objectMapper() {
        return new ObjectMapper()
                .findAndRegisterModules()
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }
}
