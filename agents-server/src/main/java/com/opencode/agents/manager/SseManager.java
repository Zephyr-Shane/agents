package com.opencode.agents.manager;

import com.opencode.agents.domain.entity.Message;
import com.opencode.agents.mapper.MessageMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Component
@RequiredArgsConstructor
public class SseManager {

    private final MessageMapper messageMapper;
    private final ExecutorService executor = Executors.newCachedThreadPool();

    public SseEmitter streamResponse(Long conversationId, AiManager aiManager,
                                     List<com.volcengine.ark.runtime.model.completion.chat.ChatMessage> messages) {
        SseEmitter emitter = new SseEmitter(5 * 60 * 1000L);

        executor.execute(() -> {
            try {
                aiManager.streamChat(messages, new AiManager.ChatStreamCallback() {
                    @Override
                    public void onToken(String token) {
                        sendEvent(emitter, "message", "{\"type\":\"token\",\"content\":\"" + escapeJson(token) + "\"}");
                    }

                    @Override
                    public void onComplete(String fullContent) {
                        if (conversationId != null && !fullContent.isEmpty()) {
                            Message msg = new Message();
                            msg.setConversationId(conversationId);
                            msg.setRole("assistant");
                            msg.setContent(fullContent);
                            msg.setTokens(0);
                            messageMapper.insert(msg);
                        }
                        sendEvent(emitter, "message", "{\"type\":\"done\",\"conversationId\":" + conversationId + "}");
                        emitter.complete();
                    }

                    @Override
                    public void onError(String error) {
                        sendEvent(emitter, "message", "{\"type\":\"error\",\"content\":\"" + escapeJson(error) + "\"}");
                        emitter.completeWithError(new RuntimeException(error));
                    }
                });
            } catch (Exception e) {
                log.error("SSE处理异常", e);
                sendEvent(emitter, "message", "{\"type\":\"error\",\"content\":\"服务器内部错误\"}");
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }

    private void sendEvent(SseEmitter emitter, String event, String data) {
        try {
            SseEmitter.SseEventBuilder builder = SseEmitter.event()
                    .name(event)
                    .data(data);
            emitter.send(builder);
        } catch (IOException e) {
            log.warn("SSE发送失败", e);
        }
    }

    private String escapeJson(String text) {
        return text.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

}
