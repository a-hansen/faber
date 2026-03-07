package com.comfortanalytics.faber.audit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class ChatTranscriptEntryTest {

    @Test
    void rejectsNullComponents() {
        Instant now = Instant.parse("2026-03-07T00:00:00Z");

        assertThrows(NullPointerException.class, () -> new ChatTranscriptEntry(null, "agent", "prompt", "response"));
        assertThrows(NullPointerException.class, () -> new ChatTranscriptEntry(now, null, "prompt", "response"));
        assertThrows(NullPointerException.class, () -> new ChatTranscriptEntry(now, "agent", null, "response"));
        assertThrows(NullPointerException.class, () -> new ChatTranscriptEntry(now, "agent", "prompt", null));
    }

    @Test
    void serializesToTheExpectedJsonShape() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        ChatTranscriptEntry entry = new ChatTranscriptEntry(
                Instant.parse("2026-03-07T00:00:00Z"),
                "JavaDeveloperAgent",
                "USER: Write a parser",
                "Here is a parser");

        String json = objectMapper.writeValueAsString(entry);

        assertTrue(json.contains("\"timestamp\":\"2026-03-07T00:00:00Z\""));
        assertTrue(json.contains("\"agent\":\"JavaDeveloperAgent\""));
        assertTrue(json.contains("\"prompt\":\"USER: Write a parser\""));
        assertTrue(json.contains("\"response\":\"Here is a parser\""));
        ChatTranscriptEntry roundTrip = objectMapper.readValue(json, ChatTranscriptEntry.class);
        assertEquals(entry, roundTrip);
    }
}

