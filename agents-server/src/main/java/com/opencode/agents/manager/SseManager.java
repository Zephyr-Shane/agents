package com.opencode.agents.manager;

import com.opencode.agents.domain.entity.Message;
import com.opencode.agents.mapper.MessageMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.function.Consumer;

@Slf4j
@Component
@RequiredArgsConstructor
public class SseManager {

    private final MessageMapper messageMapper;

    /**
     * 流式响应: 将 Flux<String> 中的每个 token 以 SSE 事件发送,
     * 完成后持久化 assistant 消息, 并通过 onComplete 回调通知调用方
     */
    public SseEmitter streamResponse(Long conversationId,
                                     Flux<String> flux,
                                     Consumer<String> onComplete) {
        SseEmitter emitter = new SseEmitter(5 * 60 * 1000L);
        StringBuilder fullContent = new StringBuilder();

        flux.subscribe(
                token -> {
                    fullContent.append(token);
                    sendEvent(emitter, "message",
                            "{\"type\":\"token\",\"content\":\"" + escapeJson(token) + "\"}");
                },
                error -> {
                    log.error("AI流式请求失败", error);
                    sendEvent(emitter, "message",
                            "{\"type\":\"error\",\"content\":\"" + escapeJson(error.getMessage()) + "\"}");
                    emitter.completeWithError(error);
                },
                () -> {
                    if (fullContent.length() > 0) {
                        // 保存 assistant 消息
                        if (conversationId != null) {
                            Message msg = new Message();
                            msg.setConversationId(conversationId);
                            msg.setRole("assistant");
                            msg.setContent(fullContent.toString());
                            messageMapper.insert(msg);
                        }
                        // 触发完成回调
                        if (onComplete != null) {
                            onComplete.accept(fullContent.toString());
                        }
                    }
                    sendEvent(emitter, "message",
                            "{\"type\":\"done\",\"conversationId\":" + conversationId + "}");
                    emitter.complete();
                }
        );

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
        if (text == null) return "";
        return text.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

}
