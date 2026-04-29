package org.example.ragdemo.service;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.spring.AiService;
import dev.langchain4j.service.spring.AiServiceWiringMode;
import reactor.core.publisher.Flux;

@AiService(
        wiringMode = AiServiceWiringMode.EXPLICIT,
        streamingChatModel = "openAiStreamingChatModel",
        chatMemory = "chatMemory"
)
public interface RagService {
    @SystemMessage(fromResource = "system.txt")
    public Flux<String> chat(String message);
}
