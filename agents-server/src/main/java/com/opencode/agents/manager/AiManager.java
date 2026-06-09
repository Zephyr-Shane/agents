package com.opencode.agents.manager;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.List;

@Slf4j
@Component
public class AiManager {

    private final ChatClient chatClient;
    private final ChatModel chatModel;
    private final MessageWindowChatMemory chatMemory;

    public AiManager(ChatModel chatModel) {
        this.chatModel = chatModel;
        this.chatMemory = MessageWindowChatMemory.builder()
                .maxMessages(20)
                .build();
        this.chatClient = ChatClient.builder(chatModel)
                .defaultOptions(OpenAiChatOptions.builder()
                        .streamUsage(true)
                        .build())
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(chatMemory).build()
                )
                .build();
    }

    /**
     * 流式对话（使用 MessageChatMemoryAdvisor 自动管理多轮对话记忆）
     */
    public Flux<String> streamChat(String systemPrompt, String userMessage, String conversationId) {
        return chatClient.prompt()
                .system(systemPrompt)
                .user(userMessage)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, conversationId))
                .stream()
                .content();
    }

    /**
     * 同步对话（独立 client，不使用记忆，用于标题生成等辅助任务）
     */
    public ChatResponse syncChat(List<Message> messages) {
        return ChatClient.builder(chatModel)
                .build()
                .prompt()
                .messages(messages)
                .call()
                .chatResponse();
    }

    /**
     * 将历史消息预填到对话记忆（服务重启后从 DB 恢复上下文）
     */
    public void populateMemory(String conversationId, List<Message> history) {
        for (Message msg : history) {
            chatMemory.add(conversationId, msg);
        }
    }

}
