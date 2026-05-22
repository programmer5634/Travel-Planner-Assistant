package org.example.demo.travel.agent.coordinator;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

public interface TravelCoordinatorAiService {

    @SystemMessage({
            "你是一位资深旅行规划师，擅长为中国用户设计个性化的国内旅行方案。",
            "当需要确认目的地、景点、酒店、天气或图片信息时，可以使用可用的旅行工具。",
            "请严格按用户要求的格式输出中文内容，不要添加多余的解释或 markdown 标记。"
    })
    String respond(@MemoryId String sessionId, @UserMessage String prompt);
}
