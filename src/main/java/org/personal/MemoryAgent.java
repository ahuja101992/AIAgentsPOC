package org.personal;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.oracle.OracleEmbeddingStore;
import dev.langchain4j.store.embedding.oracle.CreateOption;
import oracle.jdbc.pool.OracleDataSource;

import java.sql.SQLException;

public class MemoryAgent {

    interface Researcher {
        @SystemMessage("You are a helpful research assistant. Use the provided context to answer.")
        String answer(String query);
    }

    public static void main(String[] args) throws SQLException {

        // --- 1. SETUP THE DATABASE CONNECTION ---
        OracleDataSource dataSource = new OracleDataSource();
        dataSource.setURL("jdbc:oracle:thin:@localhost:1521/FREEPDB1");
        dataSource.setUser("agent_user");
        dataSource.setPassword("AgentPass123");

        // --- 2. CONFIGURE COMPONENTS ---

        // A. The Embedding Model (Text -> Vector)
        // We use a small, local model (AllMiniLM) so we don't pay for API calls for embeddings
        EmbeddingModel embeddingModel = new AllMiniLmL6V2EmbeddingModel();

        // B. The Vector Store (Oracle 23ai)
        EmbeddingStore<TextSegment> oracleStore = OracleEmbeddingStore.builder()
                .dataSource(dataSource)
                .embeddingTable("agent_memory_vectors", CreateOption.CREATE_IF_NOT_EXISTS)
                .build();

        // C. The Retriever (The logic to find "similar" vectors)
        ContentRetriever retriever = EmbeddingStoreContentRetriever.builder()
                .embeddingStore(oracleStore)
                .embeddingModel(embeddingModel)
                .maxResults(2) // Only bring back the top 2 most relevant memories
                .minScore(0.7) // Strict similarity threshold
                .build();
        // D. The LLM
        ChatLanguageModel model = GoogleAiGeminiChatModel.builder()
                .apiKey(System.getenv("GEMINI_API_KEY"))
                .modelName("gemini-flash-latest")
                .build();

        // --- 3. SEED SOME MEMORY (Ideally this happens separately) ---
        // Let's pretend the agent learned this yesterday.
        // We manually inject a fact into the DB.
        TextSegment knowledge = TextSegment.from("Oracle Database 23ai was released generally in 2024 and features 'AI Vector Search' as a native capability.");
        oracleStore.add(embeddingModel.embed(knowledge).content(), knowledge);

        System.out.println("Memory injected into Oracle DB.");

        // --- 4. RUN THE AGENT WITH MEMORY ---
        Researcher agent = AiServices.builder(Researcher.class)
                .chatLanguageModel(model)
                .contentRetriever(retriever) // <--- CRITICAL: This enables RAG
                .chatMemory(MessageWindowChatMemory.withMaxMessages(10))
                .build();

        // --- 5. TEST IT ---
        // We ask a question. The agent "recalls" the fact we just stored.
        String response = agent.answer("When was Oracle 23ai released?");

        System.out.println("\n--- AGENT RESPONSE ---");
        System.out.println(response);
    }
}