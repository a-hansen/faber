Act as an expert Java AI engineer. Please read the following system requirements and generate a
comprehensive, step-by-step Markdown execution plan for building Project Faber. Here are the
requirements:

Here is the finalized, battle-tested "Mega-Prompt" for Project Faber. I have woven in every single
constraint, fix, and edge-case we discovered during the dry runs.

You can copy and paste this entire block directly into your `SYSTEM_PROMPT.md` file.

---

**System Context:**
I am building a multi-agent system in Java called "Project Faber" using the LangChain4J framework.
The project is already initialized with Gradle and JUnit. Please generate the necessary Java
classes, interfaces, and configurations to satisfy the following architectural requirements. Focus
on clean code, interface-driven design, and separation of concerns.

**1. Core Orchestration & Routing (Strategy Pattern)**

* Create an Orchestrator agent that routes user requests to specialized sub-agents (e.g.,
  `JavaDeveloperAgent`, `FinancialAnalystAgent`).
* Implement a `RoutingStrategy` interface with two implementations, selectable via configuration:
* `RuleBasedRoutingStrategy`: Routes based on hardcoded task-to-agent mappings.
* `DynamicRoutingStrategy`: Uses a fast Tier 1 LLM to score task complexity and dynamically select
  the appropriate model tier and agent.
* *Constraint:* The `DynamicRoutingStrategy` must use LangChain4J's **Structured Outputs** (e.g.,
  returning a specific Java `Enum` representing the agent) rather than raw string parsing to prevent
  the LLM from returning chatty, unparseable text.

**2. High Availability & Cost-Optimized Fallbacks**

* Implement a `ModelProviderManager` that wraps LangChain4J's `ChatLanguageModel`.
* Configure a fallback registry (e.g., try Local Ollama first -> fallback to Free Gemini API ->
  fallback to Paid OpenAI API).
* *Constraint:* The manager must catch specific rate-limit/quota/timeout exceptions (like HTTP 429)
  and automatically retry the prompt with the next model. Do NOT blindly catch and swallow all
  generic `Exception` types (e.g., it should still crash on a `NullPointerException` or malformed
  request).

**3. Persistent Summarization Memory**

* Implement a custom `PersistentChatMemoryStore` that saves agent context to human-readable,
  pretty-printed JSON files named by `memoryId` (e.g., `[ProjectName]_[AgentRole].json`).
* Wrap LangChain4J's `TokenWindowChatMemory` with a custom manager.
* *Constraint:* Use a `TokenCountEstimator` to track the actual token limit (e.g., 8,000 tokens),
  NOT the raw message count. When the token limit nears, intercept the memory, extract the oldest
  70% of messages, send them to a fast Tier 1 `ContextCondenserAgent` to summarize, and replace
  those messages with the resulting summary in the memory stream.

**4. Immutable Audit Logging (Event Sourcing)**

* Implement a custom `ChatModelListener`.
* Silently intercept all raw prompts and responses for every agent and append them to a local
  JSONL (JSON Lines) file (e.g., `[ProjectName]_[AgentRole]_Transcript.jsonl`) for debugging.
* *Constraint:* You must explicitly generate this class. Do NOT use manual string formatting (
  `String.format()`) to build the JSON string. You must use Jackson's `ObjectMapper` to serialize
  the log entries to ensure quotes and newlines are safely escaped and do not corrupt the JSONL
  file.

**5. Sandboxed Filesystem Tools**

* Create a `SandboxedFileService` with LangChain4J `@Tool` annotations for reading and writing
  files.
* Take a configurable `rootPath`.
* *Constraint:* Implement strict path canonicalization. Use `Path.normalize()` and reject any
  read/write request where the resolved absolute path does not `startsWith()` the configured
  absolute `rootPath`.
* Allow the Orchestrator to instantiate this service in either `ReadOnly` or `ReadWrite` modes
  depending on the sub-agent's role.

**6. Execution Tools**

* Create a `GradleExecutionService` with a `@Tool` to run Gradle tasks.
* Use Java's `ProcessBuilder` to execute the Gradle wrapper strictly within the sandboxed
  `rootPath`.
* *Constraints:* * Dynamically check `os.name` to use `gradlew.bat` for Windows and `./gradlew` for
  Unix.
* You must use `pb.redirectErrorStream(true)` to merge standard error into standard output and
  prevent thread deadlocks.
* You must implement a `process.waitFor(timeout, TimeUnit)` (e.g., 2 to 5 minutes) so the tool does
  not hang indefinitely or return prematurely before the build finishes.

**7. Deliverables**

* Generate the core Java interfaces and classes for the above.
* Provide a complete example configuration file (e.g., `application.yml` or `.properties`) showing
  how to configure the model registry, sandbox paths, and routing strategies.
* Generate a `README.md` explaining how the architecture works, how to configure the API keys/local
  models, and how to run the Orchestrator.

**8. Developer Workflow & State Management**

* I will be maintaining a `PROJECT_STATE.md` file in the root directory to track our progress across
  multiple chat sessions.
* Whenever I ask you to "save state" or "wrap up," you must generate an updated version of
  `PROJECT_STATE.md` summarizing what we have built, key technical decisions made in this session,
  unresolved issues, and the immediate next steps.

9. Codebase Mapping & Context Preservation

* To prevent unnecessary file reading and save context window, I maintain a CODE_MAP.md file in the
  root directory.
* Your Requirement: Whenever you create a new Java class, interface, or add a new public method to
  an existing class, you must silently update CODE_MAP.md to reflect the new architecture. Keep the
  map concise: only include the package, class name, a 1-sentence description, and public method
  signatures. Do not include implementation logic.

**10. Workspace Indexing & Prompt Enrichment**

* Create a `WorkspaceMapService` that maintains a lightweight runtime index of all classes and
  public methods within the configured sandbox `rootPath`.
* *Constraint:* Keep the index compact and architecture-focused. It must include package names,
  type names, and public method signatures, but must not include full source code or implementation
  bodies.
* The Orchestrator must inject this workspace map into the **System Prompt** of developer-oriented
  agents so they have immediate architectural context without manually reading every file.
* The initial target is `JavaDeveloperAgent`, and the design should allow future developer agents to
  receive the same workspace context automatically.
