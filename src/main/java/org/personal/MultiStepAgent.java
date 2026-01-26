package org.personal;

import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage; // <--- ADD THIS IMPORT
import dev.langchain4j.service.V;
import dev.langchain4j.web.search.WebSearchEngine;
import dev.langchain4j.web.search.tavily.TavilyWebSearchEngine;
import dev.langchain4j.web.search.WebSearchTool;

public class MultiStepAgent {

    // Agent 1: The Doer
    interface JuniorAnalyst {
        @SystemMessage({
                "You are a junior research analyst.",
                "Draft a detailed answer based on the user's query.",
                "Use your search tool to find facts.",
                "Include citations."
        })
        String draftReport(String userQuery); // By default, a single String arg is treated as @UserMessage

        // FIX APPLIED HERE:
        // We moved the template variables {{draft}} and {{feedback}} into the @UserMessage.
        // This tells the LLM: "Here is the data, now do the work."
        @SystemMessage("You are fixing a report based on editor feedback.")
        @UserMessage("Original Draft: {{draft}} \n\n Editor Feedback: {{feedback}} \n\n Please rewrite the report.")
        String refineReport(@V("draft") String draft, @V("feedback") String feedback);
    }

    // Agent 2: The Critic
    interface SeniorEditor {
        @SystemMessage({
                "You are a Senior Editor at a technical publication.",
                "Critique the provided draft for: 1. Missing Citations 2. Logical Gaps 3. Tone.",
                "If the draft is good, output 'APPROVED'.",
                "If bad, output a bulleted list of feedback."
        })
            // FIX APPLIED HERE:
            // We changed @V to @UserMessage.
            // This treats the input string as the direct prompt sent to the model.
        String review(@UserMessage String draft);
    }

    public static void main(String[] args) {

        // ... (Your existing Model and Tool setup code remains the same) ...

        ChatLanguageModel fastModel = GoogleAiGeminiChatModel.builder()
                .apiKey(System.getenv("GEMINI_API_KEY"))
                .modelName("gemini-flash-latest")
                .temperature(0.0)
                .build();

        WebSearchEngine tavilySearchEngine = TavilyWebSearchEngine.builder()
                .apiKey(System.getenv("TAVILY_API_KEY"))
                .build();

        JuniorAnalyst analyst = AiServices.builder(JuniorAnalyst.class)
                .chatLanguageModel(fastModel)
                .tools(WebSearchTool.from(tavilySearchEngine))
                .build();

        SeniorEditor editor = AiServices.builder(SeniorEditor.class)
                .chatLanguageModel(fastModel)
                .build();

        // --- THE WORKFLOW LOOP ---

        String query = "What is the latest architecture of Oracle Database 23ai?";
        System.out.println("Step 1: Analyst is researching...");

        // This works because single-argument methods default to @UserMessage
        String draft = analyst.draftReport(query);
        System.out.println("--- DRAFT ---\n" + draft + "\n-------------");

        System.out.println("Step 2: Editor is reviewing...");
        String feedback = editor.review(draft); // Now explicitly passed as @UserMessage

        if (feedback.contains("APPROVED")) {
            System.out.println("Final Output: " + draft);
        } else {
            System.out.println("Feedback Received: " + feedback);

            System.out.println("Step 3: Analyst is fixing...");
            // Now passes the variables into the @UserMessage template we defined above
            String refinedDraft = analyst.refineReport(draft, feedback);
            System.out.println("--- REFINED DRAFT ---\n" + refinedDraft);
        }
    }
}