package org.example.demo.config;

import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import org.example.demo.travel.agent.coordinator.TravelCoordinatorAiService;
import org.example.demo.travel.memory.RedisChatMemoryStore;
import org.example.demo.travel.tool.TravelPlanningTools;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LangChain4jConfig {

    @Bean
    OpenAiChatModel travelChatModel(
            @Value("${langchain4j.open-ai.chat-model.base-url}") String baseUrl,
            @Value("${langchain4j.open-ai.chat-model.api-key}") String apiKey,
            @Value("${langchain4j.open-ai.chat-model.model-name}") String modelName,
            @Value("${langchain4j.open-ai.chat-model.log-requests:false}") boolean logRequests,
            @Value("${langchain4j.open-ai.chat-model.log-responses:false}") boolean logResponses
    ) {
        return OpenAiChatModel.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .modelName(modelName)
                .logRequests(logRequests)
                .logResponses(logResponses)
                .build();
    }

    @Bean
    TravelCoordinatorAiService travelCoordinatorAiService(OpenAiChatModel travelChatModel,
                                                          TravelPlanningTools tools,
                                                          RedisChatMemoryStore chatMemoryStore,
                                                          ChatMemoryProperties memoryProperties) {
        return AiServices.builder(TravelCoordinatorAiService.class)
                .chatModel(travelChatModel)
                .tools(tools)
                .chatMemoryProvider(memoryId -> MessageWindowChatMemory.builder()
                        .id(memoryId)
                        .maxMessages(memoryProperties.maxMessages())
                        .chatMemoryStore(chatMemoryStore)
                        .build())
                .build();
    }
}
