# Project Faber

Project Faber is a Java 17 multi-agent orchestration system built around an `OrchestratorAgent` that routes work, selects a persona, provisions tools, injects architecture context, and executes the task through a shared agent runtime.

Today, the repository contains the core building blocks for that flow:
- dynamic routing through typed `RoutingDecision` objects
- dynamic agent composition through `DynamicAgent`
- registry-based tool and context injection in `DefaultAgentFactory`
- a real CLI/bootstrap implementation in `com.comfortanalytics.faber.cli.FaberCli`
- YAML-backed configuration loading through `ConfigLoader`
- sandboxed file and Gradle tools
- persistent memory and summarization seams
- JSONL audit logging support

## Current repository status

The current checkout now supports the documented CLI/bootstrap contract:
- `com.comfortanalytics.faber.Main` is the executable jar entry point
- `com.comfortanalytics.faber.cli.FaberCli` contains the CLI/bootstrap implementation
- `config.yml` and `task.txt` are used by default when `--config` and `--task` are omitted
- `--config <path>` and `--task <path>` can still override those defaults
- the CLI builds an `OrchestratorAgent` and executes `OrchestratorAgent.execute(TaskRequest)`

Important current limitations:
- provider API keys are still read from `System.getenv()` rather than from a built-in `.env` loader
- the current model configuration shape covers `tier1` and `tier2`; `tier3` falls back to the configured tier-2 execution models

## 3. Configuration (`config.yml`)

### Implemented configuration contract

The Faber runtime is configured through a YAML file such as `config.yml`. The current CLI reads:
- the sandbox workspace root
- the routing mode
- the tier-1 and tier-2 provider/model mappings
- by default, `config.yml` from the current working directory unless `--config` is provided

Example `config.yml`:

```yaml
workspace:
  rootPath: C:/work/my-repo

routing:
  mode: DYNAMIC

models:
  tier1:
    router:
      provider: gemini
      model: gemini-2.0-flash
  tier2:
    executor:
      provider: openai
      model: gpt-4.1-mini
```

Supported provider kinds in the current CLI bootstrap are:
- `openai`
- `gemini`
- `anthropic`
- `ollama`

### What is implemented today

This part is now implemented in the repository:
- `ConfigLoader` parses `config.yml` (or the file passed through `--config`) into immutable config records under `com.comfortanalytics.faber.cli.config`
- `FaberCli` reads `task.txt` by default, or the file passed through `--task`
- relative `workspace.rootPath` values are resolved relative to the directory containing `config.yml`
- provider API keys are resolved from environment variables such as:
  - `OPENAI_API_KEY`
  - `GEMINI_API_KEY`
  - `ANTHROPIC_API_KEY`
  - optional `OLLAMA_BASE_URL`

## 4. Structuring Task Input Files

### Implemented task-file workflow

The CLI reads `task.txt` from the current working directory by default, or another task file from the path passed through `--task`.

That file should contain:
- the goal
- the relevant files or directories
- constraints or expectations
- any extra context the agent should keep in mind

Example `task.md`:

```markdown
# Goal

Review `src/main/java/com/example/InvoiceService.java` and identify any obvious null-safety bugs.

# Constraints

- Keep the changes minimal.
- Prefer Java 17 features only where they improve clarity.
- Do not modify public method signatures unless necessary.

# Deliverable

Provide a short summary of the issue and update the file in place if a safe fix is obvious.
```

Another example:

```markdown
# Goal

Create a new Java class `src/main/java/com/example/JsonDateParser.java`.

# Requirements

- Parse ISO-8601 dates.
- Add JUnit tests.
- Keep the implementation small and readable.
```

### How this maps to the current code

In the current architecture, the orchestrator ultimately works with a `TaskRequest` object.

The implemented CLI flow is:
1. read `task.md` or `task.txt`
2. load the file contents into memory
3. wrap the content in a `TaskRequest`
4. pass that request to `OrchestratorAgent.execute(...)`

What exists today:
- `TaskRequest` is implemented
- `OrchestratorAgent.execute(TaskRequest)` is implemented
- `FaberCli` reads the task file passed through `--task`

## 5. CLI Usage

### Supported CLI contract

The compiled Faber executable supports these commands:

```bash
java -jar faber.jar
java -jar faber.jar --config config.yml --task my_task.md
```

With the checked-in jar name in this repository, that is typically:

```powershell
& '.\gradlew.bat' jar
& "$env:JAVA_HOME\bin\java.exe" -jar '.\build\libs\faber-1.0-SNAPSHOT.jar'
& "$env:JAVA_HOME\bin\java.exe" -jar '.\build\libs\faber-1.0-SNAPSHOT.jar' --config '.\config.yml' --task '.\my_task.md'
```

When that command runs, the system:
1. starts the Faber runtime
2. parses `config.yml`
3. resolves provider credentials from environment variables
4. reads the task file
5. creates a `TaskRequest`
6. builds the tool and context registries
7. routes the task
8. dynamically builds the selected agent persona, tools, and injected context
9. executes the task and prints the result to standard output

### CLI help

You can print usage information with:

```powershell
& "$env:JAVA_HOME\bin\java.exe" -jar '.\build\libs\faber-1.0-SNAPSHOT.jar' --help
```

### Programmatic execution remains available

If you want deeper embedding or custom model resolution, you can still use Faber as a library and wire the runtime in Java.

The verified programmatic execution path is:
- create an `AgentExecutionEngine`
- create tool instances such as `SandboxedFileService` and `GradleExecutionService`
- create context suppliers such as `WorkspaceCodeMapLoader` and `WorkspaceMapService`
- create `DefaultAgentFactory`
- create `OrchestratorAgent`
- call `execute(TaskRequest)`

## Workspace inputs that matter today

These files matter to the implemented CLI/runtime.

### `CODE_MAP.md`

- Used as curated architecture context.
- Loaded by `WorkspaceCodeMapLoader` when the routed decision asks for `code_map` context.
- Keep this file present in the configured workspace root for developer-oriented tasks.

### `src/main/java/**`

- Scanned by `WorkspaceMapService`.
- Used to inject a compact runtime workspace index.
- Best results come when the target code lives under `src/main/java` inside the configured workspace.

### `transcripts/**`

- The CLI now attaches `AuditLogListener` instances to configured chat models.
- Transcript files are written as JSONL under `transcripts/` inside the configured workspace root.
- Files are provider-scoped, for example `transcripts/Faber_tier2_executor_Transcript.jsonl`.

### `gradlew` / `gradlew.bat`

- Required if the routed toolset includes Gradle execution.
- `GradleExecutionService` expects to run the wrapper from the configured workspace root.

## Recommended usage today

If you are working with the repository exactly as checked in right now:
- place `config.yml` and `task.txt` in the working directory when you want zero-argument CLI execution
- use `--config` and `--task` only when you need to override those defaults
- keep provider API keys in environment variables, not in `config.yml`
- keep `CODE_MAP.md` present when you want developer-focused context injection
- keep your target source under `src/main/java` when you want workspace indexing
- use the programmatic Java wiring path only when you need custom bootstrap behavior beyond the shipped CLI

## Short version

- Project Faber now includes a top-level executable entry point in `Main` and a CLI/bootstrap implementation in `FaberCli`.
- `config.yml` and `task.txt` are the default CLI inputs, with `--config` and `--task` available as overrides.
- The built jar is executable through `java -jar`.
- Dynamic routing, dynamic agent composition, sandboxed tools, memory, audit seams, and workspace indexing are all present.
- The main remaining gaps are polish items such as a built-in `.env` loader and configurable audit/bootstrap options.
