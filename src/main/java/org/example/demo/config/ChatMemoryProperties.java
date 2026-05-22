package org.example.demo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "travel.chat-memory")
public record ChatMemoryProperties(
        int maxMessages,
        String keyPrefix
) {

    public ChatMemoryProperties {
        if (maxMessages <= 0) {
            maxMessages = 24;
        }
        if (keyPrefix == null || keyPrefix.isBlank()) {
            keyPrefix = "travel:memory:";
        }
    }
}
