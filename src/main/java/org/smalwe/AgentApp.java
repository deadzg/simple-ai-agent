package org.smalwe;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.agent.tool.ToolSpecifications;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.service.tool.DefaultToolExecutor;
import dev.langchain4j.service.tool.ToolExecutionResult;
import dev.langchain4j.service.tool.ToolExecutor;

import java.util.Arrays;
import java.util.List;

public class AgentApp {

    private static final String GEMINI_MODEL_NAME = "gemini-2.5-flash";

    public static void main(String[] args) throws NoSuchMethodException {
        // Initialize the Gemini Model
        // Use this API to check which models this API KEY could be used with:
        // curl https://generativelanguage.googleapis.com/v1beta/models?key=$GEMINI_API_KEY
        GoogleAiGeminiChatModel model = GoogleAiGeminiChatModel.builder()
                .apiKey(System.getenv("GEMINI_API_KEY"))
                .modelName(GEMINI_MODEL_NAME)
                .build();

        List<ToolSpecification> toolSpecifications = ToolSpecifications.toolSpecificationsFrom(MathTool.class);


        String systemInputText = """
                                    You are a Mathematics teach in a school. 
                                    You always amend response with a funny quote about maths.
                                """;
        SystemMessage systemMessage = SystemMessage.from(systemInputText);

        String userInputText = "I have 500 oranges and 200 bananas. How many total fruits I have?";
        UserMessage userMessage = UserMessage.from(userInputText);

        List<ChatMessage> messages = Arrays.asList(systemMessage, userMessage);

        ChatRequest chatRequest = ChatRequest.builder()
                                    .messages(messages)
                                    .toolSpecifications(toolSpecifications)
                                    .build();


        ChatResponse chatResponse = model.chat(chatRequest);

        if(chatResponse.aiMessage().hasToolExecutionRequests()) {
            List<ToolExecutionRequest> toolExecutionRequests = chatResponse.aiMessage().toolExecutionRequests();
            for(ToolExecutionRequest toolExecutionRequest: toolExecutionRequests) {
                MathTool mathTool = new MathTool();
                ToolExecutor toolExecutor = new DefaultToolExecutor(mathTool, mathTool.getClass().getDeclaredMethod(
                        toolExecutionRequest.name(), int.class, int.class));
                Long result = Long.valueOf(toolExecutor.execute(toolExecutionRequest, Long.class));
                System.out.println("Result:" + result);

                ToolExecutionResultMessage toolExecutionResultMessage = ToolExecutionResultMessage.from(toolExecutionRequest, Long.toString(result));
                ChatRequest chatRequest2 = ChatRequest.builder()
                        .messages(List.of(userMessage, chatResponse.aiMessage(), toolExecutionResultMessage))
                        .toolSpecifications(toolSpecifications)
                        .build();
                ChatResponse chatResponse2 = model.chat(chatRequest2);

                System.out.println(String.format("Final AiMessage:%s", chatResponse2.aiMessage()));
            }
        }

        System.out.println(String.format("InputAiMessage: %s \nAiMessage:%s", userInputText, chatResponse.aiMessage()));
        /* Below is a sample output:
        InputAiMessage: Add two number: 5, 6
        AiMessage:AiMessage { text = "5 + 6 = 11", thinking = null, toolExecutionRequests = [], attributes = {} }
         */
        // Accessing metadata via the .metadata() sub-object
        System.out.println("Model used: " + chatResponse.metadata().modelName()); //Model used: gemini-2.5-flash
        System.out.println("Input tokens: " + chatResponse.metadata().tokenUsage().inputTokenCount()); //Input tokens: 36
        System.out.println("Output tokens: " + chatResponse.metadata().tokenUsage().outputTokenCount()); //Output tokens: 205
        System.out.println("Finish Reason: " + chatResponse.metadata().finishReason()); //Finish Reason: STOP
    }

    /**
     * Method to call LLM using simple text message as input
     * @param chatModel
     */
    public static void llmCallUsingStringMsg(ChatModel chatModel) {
        String answer = chatModel.chat("Hello World!");
        System.out.println(answer);
    }

    /**
     * Method to call LLM using ChatMessages with System and User Message as input
     * @param chatModel
     */
    public static void llmCallUsingChatMessages(ChatModel chatModel) {
        String systemInputText = """
                                    You are a Mathematics teach in a school. 
                                    You always amend response with a funny quote about maths.
                                """;
        SystemMessage systemMessage = SystemMessage.from(systemInputText);

        String userInputText = "I have 500 oranges and 200 bananas. How many total fruits I have?";
        UserMessage userMessage = UserMessage.from(userInputText);

        List<ChatMessage> messages = Arrays.asList(systemMessage, userMessage);

        ChatResponse chatResponse = chatModel.chat(messages);

        System.out.println(String.format("InputAiMessage: %s \nAiMessage:%s", userInputText, chatResponse.aiMessage()));
        /* Below is a sample output:
        InputAiMessage: Add two number: 5, 6
        AiMessage:AiMessage { text = "5 + 6 = 11", thinking = null, toolExecutionRequests = [], attributes = {} }
         */
        // Accessing metadata via the .metadata() sub-object
        System.out.println("Model used: " + chatResponse.metadata().modelName()); //Model used: gemini-2.5-flash
        System.out.println("Input tokens: " + chatResponse.metadata().tokenUsage().inputTokenCount()); //Input tokens: 36
        System.out.println("Output tokens: " + chatResponse.metadata().tokenUsage().outputTokenCount()); //Output tokens: 205
        System.out.println("Finish Reason: " + chatResponse.metadata().finishReason()); //Finish Reason: STOP
    }
}