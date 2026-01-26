package org.personal;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ListGeminiModels {

    public static void main(String[] args) {
        // 1. Get your key from environment or hardcode for testing
        String apiKey = System.getenv("GEMINI_API_KEY");
        if (apiKey == null || apiKey.isEmpty()) {
            System.err.println("Error: GEMINI_API_KEY environment variable not set.");
            return;
        }

        // 2. Build the request to the Google Generative AI "List Models" endpoint
        String url = "https://generativelanguage.googleapis.com/v1beta/models?key=" + apiKey;

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        try {
            System.out.println("Fetching available models...");
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                printCleanList(response.body());
            } else {
                System.err.println("Failed: HTTP " + response.statusCode());
                System.err.println("Response: " + response.body());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Simple regex parser to avoid needing Jackson/Gson libraries for this snippet
    private static void printCleanList(String jsonResponse) {
        System.out.println("\n--- VALID MODEL NAMES (Copy one of these) ---");

        // Matches "name": "models/gemini-pro"
        Pattern pattern = Pattern.compile("\"name\":\\s*\"models/([^\"]+)\"");
        Matcher matcher = pattern.matcher(jsonResponse);

        while (matcher.find()) {
            String modelId = matcher.group(1); // Extract the part after "models/"

            // Filter for only the core generative models to reduce noise
            if (modelId.startsWith("gemini")) {
                System.out.println(" .modelName(\"" + modelId + "\")");
            }
        }
        System.out.println("---------------------------------------------");
    }
}
