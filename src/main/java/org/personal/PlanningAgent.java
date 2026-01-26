package org.personal;

import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.web.search.WebSearchEngine;
import dev.langchain4j.web.search.tavily.TavilyWebSearchEngine;
import dev.langchain4j.web.search.WebSearchTool;

public class PlanningAgent {

    // --- THE BRAIN (Interface) ---
    interface JuniorAnalyst {

        // THE KEY CHANGE: Explicit Planning Prompt
        // We force the model to "think aloud" inside <plan> tags first.
        @SystemMessage({
                "You are a Principal Research Architect.",
                "You MUST follow this strictly sequential process for every query:",
                "",
                "1. DECOMPOSITION: Break the user's request into distinct sub-questions.",
                "2. PLANNING: Create a step-by-step plan to answer each sub-question.",
                "   - Output this plan inside <plan>...</plan> XML tags.",
                "3. EXECUTION: Use the 'search' tool to gather facts for each step of your plan.",
                "   - Do NOT answer from memory. You MUST search.",
                "4. SYNTHESIS: Compile the gathered facts into a final answer.",
                "",
                "Format the final output as:",
                "<plan>",
                "1. Search for X...",
                "2. Search for Y...",
                "</plan>",
                "",
                "--- RESEARCH REPORT ---",
                "(Your final answer here with citations)"
        })
        String research(@UserMessage String userQuery);
    }

    // --- THE MAIN LOOP ---
    public static void main(String[] args) {

        // 1. Configure the Brain (Gemini Flash is fast and good for planning)
        ChatLanguageModel model = GoogleAiGeminiChatModel.builder()
                .apiKey(System.getenv("GEMINI_API_KEY"))
                .modelName("gemini-flash-latest")
                .temperature(0.0) // Keep it deterministic
                .build();

        // 2. Configure the Tool (Tavily)
        WebSearchEngine tavily = TavilyWebSearchEngine.builder()
                .apiKey(System.getenv("TAVILY_API_KEY"))
                .build();

        // 3. Wire the Agent
        JuniorAnalyst analyst = AiServices.builder(JuniorAnalyst.class)
                .chatLanguageModel(model)
                .tools(WebSearchTool.from(tavily))
                .chatMemory(MessageWindowChatMemory.withMaxMessages(10))
                .build();

        // 4. Execute a Complex Query that requires planning
        // A simple query like "What is 2+2" doesn't need planning.
        // A complex comparison query forces the agent to use the <plan>.
        String complexQuery = "Compare the latest features of Java 21 vs Java 23. Which one is better for AI development?";

        System.out.println("Agent is thinking and planning...");
        String response = analyst.research(complexQuery);

        System.out.println("n================ AGENT RESPONSE ================\n");
        System.out.println(response);
    }
}