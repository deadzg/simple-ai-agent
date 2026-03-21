package org.smalwe;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.agent.tool.ToolSpecifications;
import dev.langchain4j.chain.ConversationalChain;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.tool.DefaultToolExecutor;
import dev.langchain4j.service.tool.ToolExecutor;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;

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

        llmCallUsingEasyRAG(model);
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

        printChatResponse(chatResponse);
    }

    public static void llmCallUsingToolCall(ChatModel chatModel) throws NoSuchMethodException {
        String systemInputText = """
                                    You are a Mathematics teach in a school. 
                                    You always amend response with a funny quote about maths.
                                """;
        SystemMessage systemMessage = SystemMessage.from(systemInputText);

        String userInputText = "I have 500 oranges and 200 bananas. How many total fruits I have?";
        UserMessage userMessage = UserMessage.from(userInputText);

        //Specify set of tools you would like LLM to pick from
        List<ToolSpecification> toolSpecifications = ToolSpecifications.toolSpecificationsFrom(MathTool.class);

        // Prepare list of messages: system and user you need to pass to LLM
        List<ChatMessage> messages = Arrays.asList(systemMessage, userMessage);

        // Create a chat request with messages and tool specification
        ChatRequest chatRequest = ChatRequest.builder()
                .messages(messages)
                .toolSpecifications(toolSpecifications)
                .build();


        // Call LLM with the chatRequest message
        ChatResponse chatResponse = chatModel.chat(chatRequest);
        printChatResponse(chatResponse);
        /*
           Check if the response has the tool execution requests

           Sample AIMessage Looks like below:
           AiMessage { text = null, thinking = null, toolExecutionRequests = [ToolExecutionRequest { id = null, name = "addNumbers", arguments = "{"arg1":200,"arg0":500}" }], attributes = {} }
         */
        if(chatResponse.aiMessage().hasToolExecutionRequests()) {

            // Fetch all the tool executions LLM has determined based on the chatRequest
            List<ToolExecutionRequest> toolExecutionRequests = chatResponse.aiMessage().toolExecutionRequests();

            //For each tool call the appropriate tool
            for(ToolExecutionRequest toolExecutionRequest: toolExecutionRequests) {
                // Create an instance of math tool
                MathTool mathTool = new MathTool();

                // Create a default tool executor based on the name and parameters of the tool method
                ToolExecutor toolExecutor = new DefaultToolExecutor(mathTool, mathTool.getClass().getDeclaredMethod(
                        toolExecutionRequest.name(), int.class, int.class));

                // Store the result of tool execution
                Long result = Long.valueOf(toolExecutor.execute(toolExecutionRequest, Long.class));
                System.out.println("Result:" + result);

                // Create the result message so that it could be sent back to LLM
                ToolExecutionResultMessage toolExecutionResultMessage = ToolExecutionResultMessage.from(toolExecutionRequest, Long.toString(result));

                // Create another chatRequest with the tool result. Make sure you send all the tools and messages again in this request along with tool result
                ChatRequest chatRequest2 = ChatRequest.builder()
                        .messages(List.of(userMessage, chatResponse.aiMessage(), toolExecutionResultMessage))
                        .toolSpecifications(toolSpecifications)
                        .build();

                // Call the LLM
                ChatResponse chatResponse2 = chatModel.chat(chatRequest2);

                // Print the LLM response
                printChatResponse(chatResponse2);
            }
        }
    }

    public static void llmCallUsingSimpleAssistant(ChatModel chatModel) {
        // Create instance of AI Service
        SimpleAssistant simpleAssistant = AiServices.create(SimpleAssistant.class, chatModel);

        // Make a call to LLM
        String result = simpleAssistant.chat("What is today's date?");
        System.out.println(result);
    }

    public static void llmCallUsingChatMemory(ChatModel chatModel) {
        ChatMemory chatMemory = MessageWindowChatMemory.withMaxMessages(5);

        ConversationalChain conversationalChain = ConversationalChain.builder()
                .chatMemory(chatMemory)
                .chatModel(chatModel)
                .build();

        String result = conversationalChain.execute("Who directed Mission Impossible Movie?");
        System.out.println(result);

        result = conversationalChain.execute("Who is the cast in the movie?");
        System.out.println(result);
    }

    public static void llmCallUsingEasyRAG(ChatModel chatModel) {
        List<Document> documents = FileSystemDocumentLoader.loadDocuments("src/main/resources/");

        InMemoryEmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();
        EmbeddingStoreIngestor.ingest(documents, embeddingStore);

        SimpleAssistant simpleAssistant = AiServices.builder(SimpleAssistant.class)
                                            .chatModel(chatModel)
                                            .chatMemory(MessageWindowChatMemory.withMaxMessages(10))
                                            .contentRetriever(EmbeddingStoreContentRetriever.from(embeddingStore))
                                            .build();
        String result = simpleAssistant.chat("get docker container logs");
        System.out.println(result);
    }

    private static void printChatResponse(ChatResponse chatResponse) {
        System.out.println(String.format("AiMessage:%s", chatResponse.aiMessage()));
        System.out.println("Model used: " + chatResponse.metadata().modelName()); //Model used: gemini-2.5-flash
        System.out.println("Input tokens: " + chatResponse.metadata().tokenUsage().inputTokenCount()); //Input tokens: 36
        System.out.println("Output tokens: " + chatResponse.metadata().tokenUsage().outputTokenCount()); //Output tokens: 205
        System.out.println("Finish Reason: " + chatResponse.metadata().finishReason()); //Finish Reason: STOP
    }
}