package com.comfortanalytics.faber;

import com.comfortanalytics.faber.orchestrator.OrchestratorAgent;
import com.comfortanalytics.faber.orchestrator.TaskRequest;
import com.comfortanalytics.faber.routing.AgentRole;
import com.comfortanalytics.faber.routing.RuleBasedRoutingStrategy;
import java.time.Instant;
import java.util.Map;

public final class Faber {

    public static void main(String[] args) {
        OrchestratorAgent orchestrator = new OrchestratorAgent(new RuleBasedRoutingStrategy());

        // Build a minimal request for the demo entry point.
        String userInput = args.length == 0
                ? "Please write a Java method to parse JSON."
                : String.join(" ", args);
        TaskRequest request = new TaskRequest("cli-demo", userInput, Map.of("source", "main"), Instant.now());

        // Route the request and show the selected agent role.
        AgentRole role = orchestrator.route(request);
        System.out.println("Routed to: " + role);
    }
}