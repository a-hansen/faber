# Project Faber

Project Faber is a Java 17 Gradle project for a multi-agent orchestration framework built around routing, model selection, specialized agents, persistent memory, sandboxed tools, immutable audit logging, dynamic routing, orchestrated agent execution, and runtime workspace indexing.

## Current status

Implemented so far:
- Phase 1 foundations
  - `TaskRequest`
  - `RoutingStrategy`
  - `RuleBasedRoutingStrategy`
  - `OrchestratorAgent`
- Phase 2 model layer
  - `ModelTier`
  - `ModelProviderConfig`
  - `ModelProvider`
  - `ModelProviderManager`
  - retryable fallback rules for timeout, rate-limit, and HTTP 429 failures
- Phase 3 agents
  - `BaseAgent`
  - `JavaDeveloperAgent`
  - `FinancialAnalystAgent`
  - shared prompt construction on top of `ModelProviderManager`
- Phase 4 memory
  - `MemoryMessage`
  - `MemoryConfig`
  - `PersistentChatMemoryStore`
  - `MemoryManager`
  - `ContextCondenserAgent`
  - JSON-backed persisted conversation memory with summarization thresholds
- Phase 5 tools
  - `SandboxedFileService`
  - `GradleExecutionService`
  - strict path sandboxing, read-only/read-write modes, and Gradle task execution with timeout protection
- Phase 6 audit logging
  - `ChatTranscriptEntry`
  - `AuditLogListener`
  - JSONL transcript persistence for prompts, responses, and model errors
- Phase 7 dynamic routing
  - `RoutingDecision`
  - `RoutingDecisionClient`
  - `DynamicRoutingStrategy`
  - `LangChain4jRoutingDecisionClient`
  - structured-output routing with typed fallback to rule-based routing
- Phase 8 orchestrator
  - `AgentExecutionEngine`
  - `LangChain4jAgentExecutionEngine`
  - `WorkspaceCodeMapLoader`
  - `AgentFactory`
  - `DefaultAgentFactory`
  - orchestrated execution pipeline with agent instantiation, tool injection, and dynamic `CODE_MAP.md` system-message context for developer agents
- Phase 9 workspace indexing
  - `WorkspaceMapService`
  - runtime Java source indexing of packages, public types, and public methods under the workspace root
  - orchestrator-driven injection of fresh workspace map context into developer-agent system prompts

## Project layout

- `src/main/java/com/comfortanalytics/faber/orchestrator`
- `src/main/java/com/comfortanalytics/faber/routing`
- `src/main/java/com/comfortanalytics/faber/model`
- `src/main/java/com/comfortanalytics/faber/agents`
- `src/main/java/com/comfortanalytics/faber/memory`
- `src/main/java/com/comfortanalytics/faber/tools`
- `src/main/java/com/comfortanalytics/faber/audit`
- `src/test/java/com/comfortanalytics/faber`

## Run tests

```powershell
& '.\gradlew.bat' test
```

## Run the demo entry point

```powershell
& '.\gradlew.bat' classes
& "$env:JAVA_HOME\bin\java.exe" -cp '.\build\classes\java\main' com.comfortanalytics.faber.Faber Analyze this stock portfolio for earnings risk
```

## Expected demo output

```text
Routed to: FINANCIAL_ANALYST
```
