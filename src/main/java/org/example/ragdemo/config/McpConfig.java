package org.example.ragdemo.config;

import dev.langchain4j.mcp.client.DefaultMcpClient;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.mcp.client.transport.McpTransport;
import dev.langchain4j.mcp.client.transport.stdio.StdioMcpTransport;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class McpConfig {
   @Bean
    public McpClient mcpClient(){
       //创建基于标准输入输出的MCP传输层
       McpTransport transport=new StdioMcpTransport.Builder()
               .command(List.of("/usr/bin/npm","exec","@modelcontextprotocol/server-everything@0.6.2"))
               .logEvents(true)
               .build();
       //创建MCP客户端
       McpClient client=new DefaultMcpClient.Builder()
               .transport(transport)
               .build();
       return client;
   }

}
