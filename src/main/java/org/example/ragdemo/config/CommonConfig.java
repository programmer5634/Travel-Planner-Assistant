package org.example.ragdemo.config;

import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import org.example.ragdemo.service.RagService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CommonConfig {
@Bean
    public ChatMemory chatMemory(){
    return MessageWindowChatMemory.builder()
            .maxMessages(20)
            .build();
}

}
