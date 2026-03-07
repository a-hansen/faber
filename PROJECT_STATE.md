# PROJECT_STATE

## Session Summary

This session completed the remaining architecture work for **Phase 8 (Orchestrator)** and **Phase 9 (Workspace Indexing)**.

### Built this session

#### Phase 8 - Orchestrator
- Added a real orchestration execution path in `OrchestratorAgent` via `execute(TaskRequest)`.
- Added `AgentFactory` and `DefaultAgentFactory` to instantiate routed agents and inject tools.
- Added `AgentExecutionRequest` and `AgentExecutionEngine` as the shared execution seam.
- Added `LangChain4jAgentExecutionEngine` for LangChain4j-based system-message and tool execution.
- Added `ModelProviderManagerAgentExecutionEngine` to preserve compatibility with the current string-based model layer.
- Refactored `BaseAgent` to build separate system and user messages and support model-tier overrides.
- Updated `JavaDeveloperAgent` so it can receive injected tools and dynamic architecture context.
- Preserved simpler construction paths for `FinancialAnalystAgent` and `ContextCondenserAgent`.

#### Phase 9 - Workspace Indexing
- Added `WorkspaceMapService` to scan the workspace root for Java packages, public types, and public methods.
- Wired `WorkspaceMapService` into the developer-agent prompt path through `BaseAgent` and `JavaDeveloperAgent`.
- Kept `CODE_MAP.md` enrichment intact and added runtime workspace-map enrichment as a separate prompt section.
- Updated `DefaultAgentFactory` so orchestrator-created developer agents receive the shared runtime workspace map.

## Current Project Status

Implemented phases:
- Phase 1 - Foundations
- Phase 2 - Model Layer
- Phase 3 - Agents
- Phase 4 - Memory
- Phase 5 - Tools
- Phase 6 - Audit Logging
- Phase 7 - Dynamic Routing
- Phase 8 - Orchestrator
- Phase 9 - Workspace Indexing

### Current capabilities
- Rule-based and dynamic routing are both available.
- Dynamic routing returns typed `RoutingDecision` objects with `AgentRole` and `ModelTier`.
- The orchestrator can route and execute the selected agent.
- Developer agents receive injected tools plus both curated and runtime architectural context before execution.
- Memory, audit logging, and sandbox/tool services are present and covered by tests.

## Verification

Validated during this session:
- Focused Phase 9 tests passed.
- Clean full-project Gradle test run passed.
- `JavaDeveloperAgentTest`, `WorkspaceMapServiceTest`, and `OrchestratorAgentLifecycleTest` all passed with zero failures.
- `CODE_MAP.md` was synchronized to include the public API added in this session.

## Technical Debt / Known Limitations

### 1. `WorkspaceMapService` is regex-based
- It uses lightweight text parsing rather than a real Java parser/AST.
- This is intentionally simple and fast, but it may become brittle with more complex signatures, annotations, nested public types, or unusual formatting.

### 2. Workspace indexing is rebuilt on demand
- The workspace map is regenerated each time a developer-agent system message is built.
- This keeps the behavior fresh and simple, but there is no caching or file-change invalidation yet.

### 3. Runtime composition is still minimal
- The project now has the orchestration and execution seams, but there is not yet a full configuration-driven composition root that wires real chat models, audit listeners, routing mode selection, and execution engines into a single runnable assistant runtime.

### 4. Audit logging is implemented but not fully composed into a live runtime path
- `AuditLogListener` exists and is tested.
- A future runtime/bootstrap layer should attach it when real LangChain4j chat models are created.

### 5. Entry point is still demo-oriented
- `Faber.java` still demonstrates routing only.
- It does not yet bootstrap the full execution pipeline with tools, runtime indexing, and real model execution.

## Unresolved Bugs

No known failing tests or blocking defects at wrap-up.

Minor caveats:
- Gradle output capture in PowerShell can be noisy/quirky, but the actual builds and JUnit XML results were verified.
- The workspace-map formatter is intentionally compact and may need refinement if prompt size becomes an issue.

## Recommended Next Step

### Next session goal
Build a **real application bootstrap/composition layer** that wires the completed architecture into an end-to-end runnable assistant.

Concrete next step:
1. Add configuration-backed runtime wiring for routing mode, workspace root, and model selection.
2. Create the real `ChatModel` resolver used by `LangChain4jAgentExecutionEngine`.
3. Attach `AuditLogListener` during model creation.
4. Update `Faber.java` (or add a dedicated bootstrap class) to run the full orchestrator execution path instead of routing-only demo behavior.
5. Run an end-to-end smoke test where the Java developer agent receives tools plus `CODE_MAP.md` and runtime workspace-map context.

