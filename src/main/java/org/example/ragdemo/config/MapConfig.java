package org.example.ragdemo.config;

import dev.langchain4j.mcp.McpToolProvider;
import dev.langchain4j.mcp.client.DefaultMcpClient;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.mcp.client.transport.McpTransport;
import dev.langchain4j.mcp.client.transport.http.HttpMcpTransport;
import dev.langchain4j.service.tool.ToolProvider;
import org.springframework.context.annotation.Bean;

import java.util.List;

public class MapConfig {
    @Bean
    public McpTransport mcpTransport() {
        return new HttpMcpTransport.Builder()
                .sseUrl("http://localhost:3001/sse")
                .logRequests(true)
                .logResponses(true)
                .build();
    }

    @Bean
    public McpClient mcpClient(McpTransport mcpTransport) {
        return new DefaultMcpClient.Builder()
                .transport(mcpTransport)
                .build();
    }

    /**
     * 👇 这个 Bean 名字非常重要，后面 @AiService 要用
     */
    @Bean(name = "mcpToolProvider")
    public ToolProvider mcpToolProvider(McpClient mcpClient) {
        return McpToolProvider.builder()
                .mcpClients(List.of(mcpClient))
                .build();
    }

}
