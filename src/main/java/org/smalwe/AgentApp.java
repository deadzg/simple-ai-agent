package org.smalwe;

import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;

public class AgentApp {

    public static void main(String[] args) {
        // Initialize the Gemini Model
        // Use this API to check which models this API KEY could be used with:
        // curl https://generativelanguage.googleapis.com/v1beta/models?key=$GEMINI_API_KEY
        GoogleAiGeminiChatModel model = GoogleAiGeminiChatModel.builder()
                .apiKey(System.getenv("GEMINI_API_KEY"))
                .modelName("gemini-2.5-flash")
                .build();

        String answer = model.chat("Hello World!");
        System.out.println(answer);
    }
}