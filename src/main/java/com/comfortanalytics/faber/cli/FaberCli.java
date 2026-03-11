package com.comfortanalytics.faber.cli;

import com.comfortanalytics.faber.agents.AgentExecutionEngine;
import com.comfortanalytics.faber.agents.LangChain4jAgentExecutionEngine;
import com.comfortanalytics.faber.agents.WorkspaceCodeMapLoader;
import com.comfortanalytics.faber.agents.WorkspaceMapService;
import com.comfortanalytics.faber.annotation.Nonnull;
import com.comfortanalytics.faber.audit.AuditLogListener;
import com.comfortanalytics.faber.cli.config.FaberConfig;
import com.comfortanalytics.faber.cli.config.ModelsConfig;
import com.comfortanalytics.faber.cli.config.ProviderConfig;
import com.comfortanalytics.faber.model.HttpException;
import com.comfortanalytics.faber.model.ModelProvider;
import com.comfortanalytics.faber.model.ModelProviderConfig;
import com.comfortanalytics.faber.model.ModelProviderManager;
import com.comfortanalytics.faber.model.ModelTier;
import com.comfortanalytics.faber.model.RateLimitException;
import com.comfortanalytics.faber.orchestrator.AgentFactory;
import com.comfortanalytics.faber.orchestrator.DefaultAgentFactory;
import com.comfortanalytics.faber.orchestrator.OrchestratorAgent;
import com.comfortanalytics.faber.orchestrator.TaskRequest;
import com.comfortanalytics.faber.routing.DynamicRoutingStrategy;
import com.comfortanalytics.faber.routing.LangChain4jRoutingDecisionClient;
import com.comfortanalytics.faber.routing.RuleBasedRoutingStrategy;
import com.comfortanalytics.faber.routing.RoutingStrategy;
import com.comfortanalytics.faber.tools.GradleExecutionService;
import com.comfortanalytics.faber.tools.SandboxedFileService;
import dev.langchain4j.model.chat.Capability;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.ChatRequestParameters;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.anthropic.AnthropicChatModel;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

public final class FaberCli {

    private static final int HTTP_TOO_MANY_REQUESTS = 429;
    private static final String SOURCE_CLI = "cli";
    private static final String DEFAULT_CONFIG_FILE = "config.yml";
    private static final String DEFAULT_TASK_FILE = "task.txt";
    private static final String AUDIT_DIRECTORY = "transcripts";
    private static final String AUDIT_FILE_PREFIX = "Faber_";
    private static final String AUDIT_FILE_SUFFIX = "_Transcript.jsonl";
    private static final String TOOL_FILE_SYSTEM = "file_system";
    private static final String TOOL_GRADLE = "gradle";
    private static final String CONTEXT_AI_CODE_MAP = "ai_code_map";
    private static final String CONTEXT_WORKSPACE_INDEX = "workspace_index";
    private static final String ROUTING_DYNAMIC = "DYNAMIC";
    private static final String ROUTING_RULE_BASED = "RULE_BASED";
    private static final String USAGE = """
            Usage:
              java -jar faber.jar [--config <path-to-config.yml>] [--task <path-to-task-file>]
              java -jar faber.jar --help

            Defaults:
              --config config.yml
              --task task.txt
            """;

    private final ConfigLoader configLoader;
    private final Map<String, String> environment;
    private final PrintStream out;
    private final PrintStream err;
    private final Bootstrapper bootstrapper;
    private final Path workingDirectory;

    public FaberCli() {
        this(new ConfigLoader(), System.getenv(), System.out, System.err, FaberCli::bootstrapRuntime, Path.of(""));
    }

    FaberCli(
            @Nonnull ConfigLoader configLoader,
            @Nonnull Map<String, String> environment,
            @Nonnull PrintStream out,
            @Nonnull PrintStream err,
            @Nonnull Bootstrapper bootstrapper) {
        this(configLoader, environment, out, err, bootstrapper, Path.of(""));
    }

    FaberCli(
            @Nonnull ConfigLoader configLoader,
            @Nonnull Map<String, String> environment,
            @Nonnull PrintStream out,
            @Nonnull PrintStream err,
            @Nonnull Bootstrapper bootstrapper,
            @Nonnull Path workingDirectory) {
        this.configLoader = Objects.requireNonNull(configLoader, "configLoader");
        this.environment = Map.copyOf(Objects.requireNonNull(environment, "environment"));
        this.out = Objects.requireNonNull(out, "out");
        this.err = Objects.requireNonNull(err, "err");
        this.bootstrapper = Objects.requireNonNull(bootstrapper, "bootstrapper");
        this.workingDirectory = Objects.requireNonNull(workingDirectory, "workingDirectory").toAbsolutePath().normalize();
    }

    public static void main(String[] args) {
        int exitCode = new FaberCli().run(args);
        if (exitCode != 0) {
            System.exit(exitCode);
        }
    }

    int run(@Nonnull String[] args) {
        try {
            CliArguments cliArguments = parseArguments(Objects.requireNonNull(args, "args"));
            if (cliArguments.showHelp()) {
                out.print(USAGE);
                return 0;
            }

            // Load the YAML configuration and task text before bootstrapping the runtime.
            Path configPath = resolveCliPath(cliArguments.configPath());
            FaberConfig config = configLoader.load(configPath);
            Path taskPath = resolveCliPath(cliArguments.taskPath());
            String taskContents = Files.readString(taskPath, StandardCharsets.UTF_8);
            Path configDirectory = resolveConfigDirectory(configPath);

            // Build the orchestrator runtime from config and execute the requested task.
            OrchestratorAgent orchestrator = bootstrapper.bootstrap(config, environment, configDirectory);
            TaskRequest request = new TaskRequest(
                    buildRequestId(taskPath),
                    taskContents,
                    Map.of(
                            "source", SOURCE_CLI,
                            "taskPath", taskPath.toString()),
                    Instant.now());
            out.println(orchestrator.execute(request));
            return 0;
        } catch (IllegalArgumentException | IllegalStateException | IOException | TimeoutException e) {
            err.println("Project Faber CLI failed: " + e.getMessage());
            err.print(USAGE);
            return 1;
        }
    }

    @Nonnull
    static OrchestratorAgent bootstrapRuntime(
            @Nonnull FaberConfig config,
            @Nonnull Map<String, String> environment,
            @Nonnull Path configDirectory) {
        return bootstrapRuntime(config, environment, configDirectory, FaberCli::createChatModel);
    }

    @Nonnull
    static OrchestratorAgent bootstrapRuntime(
            @Nonnull FaberConfig config,
            @Nonnull Map<String, String> environment,
            @Nonnull Path configDirectory,
            @Nonnull ChatModelFactory chatModelFactory) {
        FaberConfig nonNullConfig = Objects.requireNonNull(config, "config");
        Map<String, String> nonNullEnvironment = Map.copyOf(Objects.requireNonNull(environment, "environment"));
        Path nonNullConfigDirectory = Objects.requireNonNull(configDirectory, "configDirectory").toAbsolutePath().normalize();
        ChatModelFactory nonNullChatModelFactory = Objects.requireNonNull(chatModelFactory, "chatModelFactory");

        Path workspaceRoot = resolveWorkspaceRoot(nonNullConfig.workspace().rootPath(), nonNullConfigDirectory);
        String routingMode = normalizeRoutingMode(nonNullConfig.routing().mode());
        Map<ModelTier, List<ConfiguredChatModel>> configuredModels = configureModels(
                nonNullConfig.models(),
                nonNullEnvironment,
                routingMode,
                workspaceRoot,
                nonNullChatModelFactory);

        // Build both model-management and tool-aware execution seams from the configured providers.
        ModelProviderManager modelProviderManager = createModelProviderManager(configuredModels);
        AgentExecutionEngine executionEngine = new LangChain4jAgentExecutionEngine(
                tier -> fallbackChatModel(tier, configuredModels));
        Map<String, Object> toolRegistry = createToolRegistry(workspaceRoot);
        Map<String, Supplier<String>> contextRegistry = createContextRegistry(workspaceRoot);
        AgentFactory agentFactory = new DefaultAgentFactory(executionEngine, toolRegistry, contextRegistry);
        RoutingStrategy routingStrategy = createRoutingStrategy(routingMode, configuredModels);

        Objects.requireNonNull(modelProviderManager, "modelProviderManager");
        return new OrchestratorAgent(routingStrategy, agentFactory);
    }

    @Nonnull
    private static Map<ModelTier, List<ConfiguredChatModel>> configureModels(
            ModelsConfig modelsConfig,
            Map<String, String> environment,
            String routingMode,
            @Nonnull Path workspaceRoot,
            ChatModelFactory chatModelFactory) {
        ModelsConfig nonNullModelsConfig = Objects.requireNonNull(modelsConfig, "modelsConfig");
        Path nonNullWorkspaceRoot = Objects.requireNonNull(workspaceRoot, "workspaceRoot");
        List<ConfiguredChatModel> tier2Models = buildTierModels(
                ModelTier.TIER2_BALANCED,
                "tier2",
                nonNullModelsConfig.tier2(),
                environment,
                nonNullWorkspaceRoot,
                chatModelFactory,
                true);
        List<ConfiguredChatModel> rawTier1Models = buildTierModels(
                ModelTier.TIER1_FAST,
                "tier1",
                nonNullModelsConfig.tier1(),
                environment,
                nonNullWorkspaceRoot,
                chatModelFactory,
                false);
        if (ROUTING_DYNAMIC.equals(routingMode) && rawTier1Models.isEmpty()) {
            throw new IllegalArgumentException("Dynamic routing requires at least one models.tier1 provider");
        }

        LinkedHashMap<ModelTier, List<ConfiguredChatModel>> configuredModels = new LinkedHashMap<>(3);
        configuredModels.put(ModelTier.TIER1_FAST, rawTier1Models.isEmpty() ? tier2Models : rawTier1Models);
        configuredModels.put(ModelTier.TIER2_BALANCED, tier2Models);
        configuredModels.put(ModelTier.TIER3_POWERFUL, tier2Models);
        return Map.copyOf(configuredModels);
    }

    @Nonnull
    private static List<ConfiguredChatModel> buildTierModels(
            ModelTier tier,
            String tierName,
            Map<String, ProviderConfig> providerConfigs,
            Map<String, String> environment,
            @Nonnull Path workspaceRoot,
            ChatModelFactory chatModelFactory,
            boolean required) {
        Map<String, ProviderConfig> nonNullProviderConfigs = Objects.requireNonNull(providerConfigs, "providerConfigs");
        Path nonNullWorkspaceRoot = Objects.requireNonNull(workspaceRoot, "workspaceRoot");
        if (nonNullProviderConfigs.isEmpty()) {
            if (required) {
                throw new IllegalArgumentException("No providers configured for " + tierName);
            }
            return List.of();
        }

        ArrayList<ConfiguredChatModel> models = new ArrayList<>(nonNullProviderConfigs.size());
        for (Map.Entry<String, ProviderConfig> entry : nonNullProviderConfigs.entrySet()) {
            String providerId = tierName + "." + entry.getKey();
            ProviderConfig providerConfig = Objects.requireNonNull(entry.getValue(), "providerConfig");
            List<ChatModelListener> listeners = List.of(new AuditLogListener(auditTranscriptFile(nonNullWorkspaceRoot, providerId), providerId));
            ChatModel chatModel = chatModelFactory.create(providerId, providerConfig, environment, listeners);
            models.add(new ConfiguredChatModel(tier, providerId, providerConfig.model(), chatModel));
        }
        return List.copyOf(models);
    }

    @Nonnull
    static Path auditTranscriptFile(@Nonnull Path workspaceRoot, @Nonnull String providerId) {
        Path transcriptDirectory = Objects.requireNonNull(workspaceRoot, "workspaceRoot")
                .toAbsolutePath()
                .normalize()
                .resolve(AUDIT_DIRECTORY);
        return transcriptDirectory.resolve(AUDIT_FILE_PREFIX + sanitizeProviderId(providerId) + AUDIT_FILE_SUFFIX)
                .normalize();
    }

    @Nonnull
    private static String sanitizeProviderId(@Nonnull String providerId) {
        String sanitized = Objects.requireNonNull(providerId, "providerId")
                .trim()
                .replaceAll("[^A-Za-z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
        return sanitized.isEmpty() ? "provider" : sanitized;
    }

    @Nonnull
    private static ModelProviderManager createModelProviderManager(
            @Nonnull Map<ModelTier, List<ConfiguredChatModel>> configuredModels) {
        LinkedHashMap<ModelTier, List<ModelProviderConfig>> tierConfigs = new LinkedHashMap<>(configuredModels.size());
        LinkedHashMap<String, ModelProvider> providers = new LinkedHashMap<>();
        for (Map.Entry<ModelTier, List<ConfiguredChatModel>> entry : configuredModels.entrySet()) {
            ArrayList<ModelProviderConfig> configs = new ArrayList<>(entry.getValue().size());
            for (ConfiguredChatModel configuredModel : entry.getValue()) {
                configs.add(new ModelProviderConfig(configuredModel.providerId(), configuredModel.modelName()));
                providers.putIfAbsent(
                        configuredModel.providerId(),
                        new ChatModelBackedProvider(configuredModel.providerId(), configuredModel.chatModel()));
            }
            tierConfigs.put(entry.getKey(), List.copyOf(configs));
        }
        return new ModelProviderManager(tierConfigs, new ArrayList<>(providers.values()));
    }

    @Nonnull
    private static RoutingStrategy createRoutingStrategy(
            @Nonnull String routingMode,
            @Nonnull Map<ModelTier, List<ConfiguredChatModel>> configuredModels) {
        return switch (normalizeRoutingMode(routingMode)) {
            case ROUTING_RULE_BASED -> new RuleBasedRoutingStrategy();
            case ROUTING_DYNAMIC -> new DynamicRoutingStrategy(
                    new LangChain4jRoutingDecisionClient(fallbackChatModel(ModelTier.TIER1_FAST, configuredModels)));
            default -> throw new IllegalArgumentException("Unsupported routing mode: " + routingMode);
        };
    }

    @Nonnull
    private static Map<String, Object> createToolRegistry(@Nonnull Path workspaceRoot) {
        Path nonNullWorkspaceRoot = Objects.requireNonNull(workspaceRoot, "workspaceRoot");
        return Map.of(
                TOOL_FILE_SYSTEM, new SandboxedFileService(nonNullWorkspaceRoot, SandboxedFileService.Mode.READ_WRITE),
                TOOL_GRADLE, new GradleExecutionService(nonNullWorkspaceRoot));
    }

    @Nonnull
    private static Map<String, Supplier<String>> createContextRegistry(@Nonnull Path workspaceRoot) {
        Path nonNullWorkspaceRoot = Objects.requireNonNull(workspaceRoot, "workspaceRoot");
        WorkspaceCodeMapLoader codeMapLoader = new WorkspaceCodeMapLoader(nonNullWorkspaceRoot);
        WorkspaceMapService workspaceMapService = new WorkspaceMapService(nonNullWorkspaceRoot);
        return Map.of(
                CONTEXT_AI_CODE_MAP, codeMapLoader::loadCodeMap,
                CONTEXT_WORKSPACE_INDEX, workspaceMapService::loadWorkspaceMap);
    }

    @Nonnull
    private static Path resolveWorkspaceRoot(@Nonnull String rootPath, @Nonnull Path configDirectory) {
        Path candidate = Path.of(Objects.requireNonNull(rootPath, "rootPath"));
        Path resolvedPath = candidate.isAbsolute()
                ? candidate.toAbsolutePath().normalize()
                : Objects.requireNonNull(configDirectory, "configDirectory").resolve(candidate).toAbsolutePath().normalize();
        if (!Files.exists(resolvedPath)) {
            throw new IllegalArgumentException("Configured workspace root does not exist: " + resolvedPath);
        }
        return resolvedPath;
    }

    @Nonnull
    private static Path resolveConfigDirectory(@Nonnull Path configPath) {
        Path resolvedPath = Objects.requireNonNull(configPath, "configPath").toAbsolutePath().normalize();
        Path parent = resolvedPath.getParent();
        return parent == null ? resolvedPath.getRoot() : parent;
    }

    @Nonnull
    private static String buildRequestId(@Nonnull Path taskPath) {
        String fileName = Objects.requireNonNull(taskPath, "taskPath").getFileName().toString();
        return fileName.isBlank() ? "task" : fileName;
    }

    @Nonnull
    private Path resolveCliPath(@Nonnull Path path) {
        Path nonNullPath = Objects.requireNonNull(path, "path");
        if (nonNullPath.isAbsolute()) {
            return nonNullPath.normalize();
        }
        return workingDirectory.resolve(nonNullPath).normalize();
    }

    @Nonnull
    private static CliArguments parseArguments(@Nonnull String[] args) {
        if (args.length == 1 && "--help".equals(args[0])) {
            return new CliArguments(null, null, true);
        }

        Path configPath = Path.of(DEFAULT_CONFIG_FILE);
        Path taskPath = Path.of(DEFAULT_TASK_FILE);
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            switch (arg) {
                case "--config" -> configPath = Path.of(requireOptionValue(args, ++i, arg));
                case "--task" -> taskPath = Path.of(requireOptionValue(args, ++i, arg));
                case "--help" -> {
                    return new CliArguments(null, null, true);
                }
                default -> throw new IllegalArgumentException("Unknown argument: " + arg);
            }
        }
        return new CliArguments(configPath, taskPath, false);
    }

    @Nonnull
    private static String requireOptionValue(@Nonnull String[] args, int index, @Nonnull String option) {
        if (index >= args.length) {
            throw new IllegalArgumentException("Missing value for " + option);
        }
        String value = Objects.requireNonNull(args[index], "argument value").trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException("Blank value for " + option);
        }
        return value;
    }

    @Nonnull
    private static String normalizeRoutingMode(@Nonnull String routingMode) {
        return Objects.requireNonNull(routingMode, "routingMode")
                .trim()
                .replace('-', '_')
                .toUpperCase(Locale.ROOT);
    }

    @Nonnull
    private static ChatModel fallbackChatModel(
            @Nonnull ModelTier tier,
            @Nonnull Map<ModelTier, List<ConfiguredChatModel>> configuredModels) {
        List<ConfiguredChatModel> models = configuredModels.get(Objects.requireNonNull(tier, "tier"));
        if (models == null || models.isEmpty()) {
            throw new IllegalStateException("No chat models configured for tier: " + tier);
        }
        return new FallbackChatModel(tier, models);
    }

    @Nonnull
    private static ChatModel createChatModel(
            @Nonnull String providerId,
            @Nonnull ProviderConfig providerConfig,
            @Nonnull Map<String, String> environment,
            @Nonnull List<ChatModelListener> listeners) {
        String provider = normalizeProvider(Objects.requireNonNull(providerConfig, "providerConfig").provider());
        return switch (provider) {
            case "openai" -> buildOpenAiChatModel(providerId, providerConfig, environment, listeners);
            case "gemini" -> buildGeminiChatModel(providerId, providerConfig, environment, listeners);
            case "anthropic" -> buildAnthropicChatModel(providerId, providerConfig, environment, listeners);
            case "ollama" -> buildOllamaChatModel(providerConfig, environment, listeners);
            default -> throw new IllegalArgumentException("Unsupported model provider: " + providerConfig.provider());
        };
    }

    @Nonnull
    private static ChatModel buildOpenAiChatModel(
            @Nonnull String providerId,
            @Nonnull ProviderConfig providerConfig,
            @Nonnull Map<String, String> environment,
            @Nonnull List<ChatModelListener> listeners) {
        OpenAiChatModel.OpenAiChatModelBuilder builder = OpenAiChatModel.builder()
                .apiKey(requireEnvironmentVariable(environment, "OPENAI_API_KEY", providerId))
                .modelName(providerConfig.model())
                .listeners(Objects.requireNonNull(listeners, "listeners"));
        String baseUrl = optionalEnvironmentVariable(environment, "OPENAI_BASE_URL");
        if (baseUrl != null) {
            builder.baseUrl(baseUrl);
        }
        return builder.build();
    }

    @Nonnull
    private static ChatModel buildGeminiChatModel(
            @Nonnull String providerId,
            @Nonnull ProviderConfig providerConfig,
            @Nonnull Map<String, String> environment,
            @Nonnull List<ChatModelListener> listeners) {
        GoogleAiGeminiChatModel.GoogleAiGeminiChatModelBuilder builder = GoogleAiGeminiChatModel.builder()
                .apiKey(requireEnvironmentVariable(environment, "GEMINI_API_KEY", providerId))
                .modelName(providerConfig.model())
                .listeners(Objects.requireNonNull(listeners, "listeners"));
        String baseUrl = optionalEnvironmentVariable(environment, "GEMINI_BASE_URL");
        if (baseUrl != null) {
            builder.baseUrl(baseUrl);
        }
        return builder.build();
    }

    @Nonnull
    private static ChatModel buildAnthropicChatModel(
            @Nonnull String providerId,
            @Nonnull ProviderConfig providerConfig,
            @Nonnull Map<String, String> environment,
            @Nonnull List<ChatModelListener> listeners) {
        AnthropicChatModel.AnthropicChatModelBuilder builder = AnthropicChatModel.builder()
                .apiKey(requireEnvironmentVariable(environment, "ANTHROPIC_API_KEY", providerId))
                .modelName(providerConfig.model())
                .listeners(Objects.requireNonNull(listeners, "listeners"));
        String baseUrl = optionalEnvironmentVariable(environment, "ANTHROPIC_BASE_URL");
        if (baseUrl != null) {
            builder.baseUrl(baseUrl);
        }
        return builder.build();
    }

    @Nonnull
    private static ChatModel buildOllamaChatModel(
            @Nonnull ProviderConfig providerConfig,
            @Nonnull Map<String, String> environment,
            @Nonnull List<ChatModelListener> listeners) {
        OllamaChatModel.OllamaChatModelBuilder builder = OllamaChatModel.builder()
                .modelName(providerConfig.model())
                .listeners(Objects.requireNonNull(listeners, "listeners"));
        String baseUrl = optionalEnvironmentVariable(environment, "OLLAMA_BASE_URL");
        if (baseUrl != null) {
            builder.baseUrl(baseUrl);
        }
        return builder.build();
    }

    @Nonnull
    private static String normalizeProvider(@Nonnull String provider) {
        return Objects.requireNonNull(provider, "provider")
                .trim()
                .toLowerCase(Locale.ROOT)
                .replace('_', '-');
    }

    @Nonnull
    private static String requireEnvironmentVariable(
            @Nonnull Map<String, String> environment,
            @Nonnull String variableName,
            @Nonnull String providerId) {
        String value = optionalEnvironmentVariable(environment, variableName);
        if (value == null) {
            throw new IllegalArgumentException(
                    "Missing environment variable " + variableName + " for provider " + providerId);
        }
        return value;
    }

    private static String optionalEnvironmentVariable(@Nonnull Map<String, String> environment, @Nonnull String variableName) {
        String value = Objects.requireNonNull(environment, "environment").get(Objects.requireNonNull(variableName, "variableName"));
        if (value == null) {
            return null;
        }
        String trimmedValue = value.trim();
        return trimmedValue.isEmpty() ? null : trimmedValue;
    }

    private static boolean isRetryable(@Nonnull Throwable throwable) {
        Throwable current = Objects.requireNonNull(throwable, "throwable");
        while (current != null) {
            if (current instanceof TimeoutException || current instanceof RateLimitException) {
                return true;
            }
            if (current instanceof HttpException httpException && httpException.statusCode() == HTTP_TOO_MANY_REQUESTS) {
                return true;
            }
            String className = current.getClass().getName().toLowerCase(Locale.ROOT);
            if (className.contains("ratelimit") || className.contains("toomanyrequests") || className.contains("quota")) {
                return true;
            }
            String message = current.getMessage();
            if (message != null) {
                String normalizedMessage = message.toLowerCase(Locale.ROOT);
                if (normalizedMessage.contains("429")
                        || normalizedMessage.contains("rate limit")
                        || normalizedMessage.contains("too many requests")
                        || normalizedMessage.contains("quota")) {
                    return true;
                }
            }
            current = current.getCause();
        }
        return false;
    }

    private static boolean isTimeout(@Nonnull Throwable throwable) {
        Throwable current = Objects.requireNonNull(throwable, "throwable");
        while (current != null) {
            if (current instanceof TimeoutException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    @Nonnull
    private static RuntimeException toRuntimeException(@Nonnull Throwable throwable) {
        Throwable nonNullThrowable = Objects.requireNonNull(throwable, "throwable");
        if (nonNullThrowable instanceof RuntimeException runtimeException) {
            return runtimeException;
        }
        return new IllegalStateException(nonNullThrowable.getMessage(), nonNullThrowable);
    }

    @Nonnull
    private static TimeoutException toTimeoutException(@Nonnull Throwable throwable) {
        TimeoutException timeoutException = new TimeoutException(Objects.requireNonNull(throwable, "throwable").getMessage());
        timeoutException.initCause(throwable);
        return timeoutException;
    }

    interface Bootstrapper {

        @Nonnull
        OrchestratorAgent bootstrap(
                @Nonnull FaberConfig config,
                @Nonnull Map<String, String> environment,
                @Nonnull Path configDirectory);
    }

    interface ChatModelFactory {

        @Nonnull
        ChatModel create(
                @Nonnull String providerId,
                @Nonnull ProviderConfig providerConfig,
                @Nonnull Map<String, String> environment,
                @Nonnull List<ChatModelListener> listeners);
    }

    private record CliArguments(Path configPath, Path taskPath, boolean showHelp) {
    }

    private record ConfiguredChatModel(
            @Nonnull ModelTier tier,
            @Nonnull String providerId,
            @Nonnull String modelName,
            @Nonnull ChatModel chatModel) {

        private ConfiguredChatModel {
            Objects.requireNonNull(tier, "tier");
            Objects.requireNonNull(providerId, "providerId");
            Objects.requireNonNull(modelName, "modelName");
            Objects.requireNonNull(chatModel, "chatModel");
        }
    }

    private static final class FallbackChatModel implements ChatModel {

        private final ModelTier tier;
        private final List<ConfiguredChatModel> candidates;

        private FallbackChatModel(@Nonnull ModelTier tier, @Nonnull List<ConfiguredChatModel> candidates) {
            this.tier = Objects.requireNonNull(tier, "tier");
            this.candidates = List.copyOf(Objects.requireNonNull(candidates, "candidates"));
        }

        @Override
        @Nonnull
        public ChatResponse doChat(@Nonnull ChatRequest request) {
            RuntimeException lastRetryableFailure = null;
            for (ConfiguredChatModel candidate : candidates) {
                try {
                    return candidate.chatModel().chat(Objects.requireNonNull(request, "request"));
                } catch (RuntimeException e) {
                    if (!isRetryable(e)) {
                        throw e;
                    }
                    lastRetryableFailure = e;
                }
            }
            if (lastRetryableFailure != null) {
                throw lastRetryableFailure;
            }
            throw new IllegalStateException("No chat models configured for tier: " + tier);
        }

        @Override
        @Nonnull
        public ChatRequestParameters defaultRequestParameters() {
            return candidates.get(0).chatModel().defaultRequestParameters();
        }

        @Override
        @Nonnull
        public List<ChatModelListener> listeners() {
            return candidates.get(0).chatModel().listeners();
        }

        @Override
        @Nonnull
        public Set<Capability> supportedCapabilities() {
            return candidates.get(0).chatModel().supportedCapabilities();
        }
    }

    private static final class ChatModelBackedProvider implements ModelProvider {

        private final String providerId;
        private final ChatModel chatModel;

        private ChatModelBackedProvider(@Nonnull String providerId, @Nonnull ChatModel chatModel) {
            this.providerId = Objects.requireNonNull(providerId, "providerId");
            this.chatModel = Objects.requireNonNull(chatModel, "chatModel");
        }

        @Override
        @Nonnull
        public String providerId() {
            return providerId;
        }

        @Override
        @Nonnull
        public String generate(@Nonnull String prompt) throws TimeoutException {
            try {
                return chatModel.chat(Objects.requireNonNull(prompt, "prompt"));
            } catch (RuntimeException e) {
                if (isTimeout(e)) {
                    throw toTimeoutException(e);
                }
                if (isRetryable(e)) {
                    throw new RateLimitException("Retryable chat-model failure for provider " + providerId, e);
                }
                throw toRuntimeException(e);
            }
        }
    }
}

