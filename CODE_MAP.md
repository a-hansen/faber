# CODE_MAP

## Package `com.comfortanalytics.faber.annotation`

### `Nonnull`
Marker annotation used to document non-null parameters, return values, fields, and type uses.
- Public methods: none

## Package `com.comfortanalytics.faber.agents`

### `AgentExecutionEngine`
Interface for components that execute an agent invocation using a specific model/runtime strategy.
- `public String execute(AgentExecutionRequest request) throws TimeoutException`

### `AgentExecutionRequest`
Immutable execution record carrying a system message, user message, model tier, and injected tool list.
- `public AgentExecutionRequest(String systemMessage, String userMessage, ModelTier modelTier, List<Object> tools)`
- `public String systemMessage()`
- `public String userMessage()`
- `public ModelTier modelTier()`
- `public List<Object> tools()`

### `BaseAgent`
Abstract base class for stateless agent wrappers that build enriched system/user messages and delegate execution to an agent engine.
- `public final AgentRole role()`
- `public final ModelTier modelTier()`
- `public final String handle(TaskRequest request) throws TimeoutException`
- `public final String handle(TaskRequest request, ModelTier requestedTier) throws TimeoutException`

### `ContextCondenserAgent`
Specialized agent that condenses prior memory messages into a summary message.
- `public ContextCondenserAgent(ModelProviderManager modelProviderManager)`
- `public ContextCondenserAgent(Path workspaceRoot, AgentExecutionEngine executionEngine)`
- `public MemoryMessage condense(List<MemoryMessage> messages) throws TimeoutException`

### `DynamicAgent`
Generic agent wrapper that executes a routing-selected persona with dynamically resolved tools and injected context.
- `public DynamicAgent(AgentRole role, ModelTier modelTier, String persona, AgentExecutionEngine executionEngine, List<Object> tools, String injectedContext)`

### `LangChain4jAgentExecutionEngine`
LangChain4j-backed execution engine that resolves a chat model by tier and applies system-message and tool injection per invocation.
- `public LangChain4jAgentExecutionEngine(Function<ModelTier, ChatModel> chatModelResolver)`
- `public String execute(AgentExecutionRequest request) throws TimeoutException`

### `WorkspaceCodeMapLoader`
Utility that reads `CODE_MAP.md` from the workspace root on demand for system-message enrichment.
- `public WorkspaceCodeMapLoader(Path workspaceRoot)`
- `public String loadCodeMap()`

### `WorkspaceMapService`
Runtime indexing service that scans `src/main/java`, caches a compact package/type/public-method map, and includes nested public types for prompt enrichment.
- `public WorkspaceMapService(Path rootPath)`
- `public String loadWorkspaceMap()`

## Package `com.comfortanalytics.faber.cli`

### `ConfigLoader`
YAML-backed loader that reads a `faber.yml` file into the immutable CLI configuration model.
- `public ConfigLoader()`
- `public FaberConfig load(Path configPath)`

### `FaberCli`
CLI/bootstrap entry point that parses `--config` and `--task`, wires models/tools/contexts, attaches audit listeners, and executes the orchestrator.
- `public static void main(String[] args)`

## Package `com.comfortanalytics.faber.cli.config`

### `FaberConfig`
Top-level immutable CLI configuration record that groups workspace, routing, and model-tier settings.
- `public FaberConfig(WorkspaceConfig workspace, RoutingConfig routing, ModelsConfig models)`
- `public WorkspaceConfig workspace()`
- `public RoutingConfig routing()`
- `public ModelsConfig models()`

### `WorkspaceConfig`
Immutable workspace configuration record that defines the sandbox root path used by tools and context loaders.
- `public WorkspaceConfig(String rootPath)`
- `public String rootPath()`

### `RoutingConfig`
Immutable routing configuration record that captures the configured routing mode for the CLI runtime.
- `public RoutingConfig(String mode)`
- `public String mode()`

### `ModelsConfig`
Immutable model configuration record that maps tier aliases to provider/model definitions for tier 1 and tier 2 runtime selection.
- `public ModelsConfig(Map<String, ProviderConfig> tier1, Map<String, ProviderConfig> tier2)`
- `public Map<String, ProviderConfig> tier1()`
- `public Map<String, ProviderConfig> tier2()`

### `ProviderConfig`
Immutable provider configuration record that defines the provider kind and concrete model name for one configured entry.
- `public ProviderConfig(String provider, String model)`
- `public String provider()`
- `public String model()`

## Package `com.comfortanalytics.faber.audit`

### `AuditLogListener`
Chat model listener that appends prompt and response transcript entries to a JSONL audit file.
- `public AuditLogListener(Path transcriptFile, String agent)`
- `public void onResponse(ChatModelResponseContext context)`
- `public void onError(ChatModelErrorContext context)`

### `ChatTranscriptEntry`
Immutable transcript record that captures the timestamp, agent, prompt, and response for one audit event.
- `public ChatTranscriptEntry(Instant timestamp, String agent, String prompt, String response)`
- `public Instant timestamp()`
- `public String agent()`
- `public String prompt()`
- `public String response()`

## Package `com.comfortanalytics.faber.memory`

### `MemoryConfig`
Configuration record that defines the token limit and summarization threshold for memory management.
- `public MemoryConfig(int tokenLimit, double summarizeThreshold)`
- `public int tokenLimit()`
- `public double summarizeThreshold()`

### `MemoryCondenser`
Functional interface for turning a list of memory messages into a condensed summary message.
- `public MemoryMessage condense(List<MemoryMessage> messages) throws TimeoutException`

### `MemoryManager`
Coordinator that loads, appends, summarizes, and persists conversation memory.
- `public MemoryManager(PersistentChatMemoryStore memoryStore, MemoryConfig memoryConfig, MemoryTokenEstimator tokenEstimator, MemoryCondenser memoryCondenser)`
- `public List<MemoryMessage> load(String memoryId) throws IOException`
- `public List<MemoryMessage> append(String memoryId, MemoryMessage message) throws IOException, TimeoutException`

### `MemoryMessage`
Immutable memory record representing one stored message with a role and text body.
- `public MemoryMessage(String role, String text)`
- `public String role()`
- `public String text()`

### `MemoryTokenEstimator`
Functional interface for estimating token usage across a list of memory messages.
- `public int estimate(List<MemoryMessage> messages)`

### `PersistentChatMemoryStore`
JSON-backed store that reads and writes persisted chat memory by memory identifier.
- `public PersistentChatMemoryStore(Path rootPath)`
- `public List<MemoryMessage> load(String memoryId) throws IOException`
- `public void save(String memoryId, List<MemoryMessage> messages) throws IOException`

## Package `com.comfortanalytics.faber.model`

### `HttpException`
Runtime exception that carries an HTTP status code for provider failure handling.
- `public HttpException(int statusCode, String message)`
- `public HttpException(int statusCode, String message, Throwable cause)`
- `public int statusCode()`

### `ModelProvider`
Interface for model providers that can generate text for a prompt.
- `public String providerId()`
- `public String generate(String prompt) throws TimeoutException`

### `ModelProviderConfig`
Immutable configuration record for a model provider identifier and model name.
- `public ModelProviderConfig(String providerId, String modelName)`
- `public String providerId()`
- `public String modelName()`

### `ModelProviderManager`
Manager that selects configured providers by tier and applies ordered retryable fallback behavior.
- `public ModelProviderManager(Map<ModelTier, List<ModelProviderConfig>> tierConfigs, List<ModelProvider> providers)`
- `public String generate(ModelTier tier, String prompt) throws TimeoutException`

### `ModelTier`
Enum representing the supported model capability tiers.
- Public methods: none

### `RateLimitException`
Runtime exception used to mark retryable provider rate-limit failures.
- `public RateLimitException(String message)`
- `public RateLimitException(String message, Throwable cause)`

## Package `com.comfortanalytics.faber.orchestrator`

### `AgentFactory`
Factory interface for instantiating a routed agent from the full typed routing decision.
- `public BaseAgent create(RoutingDecision decision)`

### `DefaultAgentFactory`
Registry-backed factory that resolves required tools and context loaders before composing a `DynamicAgent`.
- `public DefaultAgentFactory(AgentExecutionEngine executionEngine, Map<String, Object> toolRegistry, Map<String, Supplier<String>> contextRegistry)`
- `public BaseAgent create(RoutingDecision decision)`

### `OrchestratorAgent`
Orchestrator that can either route a request or execute the selected agent through the full lifecycle pipeline.
- `public OrchestratorAgent(RoutingStrategy routingStrategy)`
- `public OrchestratorAgent(RoutingStrategy routingStrategy, AgentFactory agentFactory)`
- `public AgentRole route(TaskRequest request)`
- `public String execute(TaskRequest request) throws TimeoutException`

### `TaskRequest`
Immutable request object that captures request identity, input text, metadata, and timestamp.
- `public TaskRequest(String requestId, String userInput, Map<String, String> metadata, Instant timestamp)`
- `public String requestId()`
- `public String userInput()`
- `public Map<String, String> metadata()`
- `public Instant timestamp()`

## Package `com.comfortanalytics.faber.routing`

### `AgentRole`
Enum representing the agent roles available to the routing layer.
- Public methods: none

### `DynamicRoutingStrategy`
Routing strategy that uses a typed routing decision client and falls back to default decision metadata when needed.
- `public DynamicRoutingStrategy(RoutingDecisionClient routingDecisionClient)`
- `public AgentRole route(TaskRequest request)`
- `public RoutingDecision routeDecision(TaskRequest request)`

### `LangChain4jRoutingDecisionClient`
Structured-output routing client that uses LangChain4j AI Services to return a typed `RoutingDecision` with persona, tool IDs, and context IDs.
- `public LangChain4jRoutingDecisionClient(ChatModel chatModel)`
- `public RoutingDecision routeDecision(TaskRequest request)`

### `RoutingDecision`
Immutable routing result that captures the selected agent role, model tier, persona, and required tool/context identifiers.
- `public RoutingDecision(AgentRole role, ModelTier modelTier, String persona, List<String> requiredTools, List<String> requiredContexts)`
- `public AgentRole role()`
- `public ModelTier modelTier()`
- `public String persona()`
- `public List<String> requiredTools()`
- `public List<String> requiredContexts()`

### `RoutingDecisionClient`
Interface for components that classify a task request into a typed routing decision.
- `public RoutingDecision routeDecision(TaskRequest request)`

### `RoutingStrategy`
Strategy interface for selecting an agent role for a task request.
- `public AgentRole route(TaskRequest request)`

### `RuleBasedRoutingStrategy`
Routing strategy that maps requests to agent roles using keyword-based rules.
- `public AgentRole route(TaskRequest request)`

## Package `com.comfortanalytics.faber.tools`

### `GradleExecutionService`
Tool service that runs Gradle wrapper tasks inside the project sandbox with timeout protection.
- `public GradleExecutionService(Path projectRoot)`
- `public String runGradleTask(String task)`

### `SandboxedFileService`
Tool service that reads and writes UTF-8 files inside a sandboxed root path.
- `public SandboxedFileService(Path rootPath, SandboxedFileService.Mode mode)`
- `public String readFile(String userPath)`
- `public String writeFile(String userPath, String content)`

### `SandboxedFileService.Mode`
Enum representing whether the sandboxed file service is read-only or read-write.
- Public methods: none
