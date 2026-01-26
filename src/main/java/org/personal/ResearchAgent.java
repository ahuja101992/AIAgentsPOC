package org.personal;

import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.web.search.WebSearchEngine;
import dev.langchain4j.web.search.tavily.TavilyWebSearchEngine;
import dev.langchain4j.web.search.WebSearchTool; // The adapter

public class ResearchAgent {

    // 1. Define the Agent Interface
    interface Researcher {
        @SystemMessage({
                "You are a Principal Technical Analyst.",
                "If you do not know the answer, you MUST use the search tool.",
                "Do not guess. Verify facts before answering.",
                "Format your answer with bullet points and cite sources."
        })
        String answer(String query);
    }

    public static void main(String[] args) {

        // 2. Configure the "Brain"

        GoogleAiGeminiChatModel model = GoogleAiGeminiChatModel.builder()
                .apiKey(System.getenv("GEMINI_API_KEY")) // Use your new key here
                .modelName("gemini-flash-latest")
                .temperature(0.0) // 0.0 is best for factual research/tool use
                .build();

        // 3. Configure the "Tool" (Tavily)
        WebSearchEngine tavily = TavilyWebSearchEngine.builder()
                .apiKey(System.getenv("TAVILY_API_KEY"))
                .build();

        // 4. Wire it together using AiServices (The Magic Layer)
        Researcher agent = AiServices.builder(Researcher.class)
                .chatLanguageModel(model)
                // This adapter automatically exposes "searchWeb" to the LLM
                .tools(WebSearchTool.from(tavily))
                .chatMemory(MessageWindowChatMemory.withMaxMessages(10))
                .build();

        // 5. Run it
        System.out.println("Thinking...");
        String result = agent.answer("What is the current stock price of Oracle (ORCL) and what are the recent analyst ratings from 2024-2025?");

        System.out.println(result);
    }
}