package com.opencode.agents.manager;

import com.volcengine.ark.runtime.model.completion.chat.*;
import com.volcengine.ark.runtime.service.ArkService;
import io.reactivex.schedulers.Schedulers;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Component
public class AiManager {

    @Resource
    private ArkService arkService;

    private static final String MODEL_ID = "doubao-1.5-vision-pro-250328";

    public void streamChat(List<ChatMessage> messages, ChatStreamCallback callback) {
        ChatCompletionRequest request = ChatCompletionRequest.builder()
                .model(MODEL_ID)
                .messages(messages)
                .maxTokens(4096)
                .streamOptions(ChatCompletionRequest.ChatCompletionRequestStreamOptions.of(true))
                .temperature(0.6)
                .topP(0.8)
                .build();

        StringBuilder fullContent = new StringBuilder();
        CountDownLatch latch = new CountDownLatch(1);

        arkService.streamChatCompletion(request)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.single())
                .subscribe(
                        chunk -> {
                            if (!chunk.getChoices().isEmpty()) {
                                ChatMessage msg = chunk.getChoices().get(0).getMessage();
                                if (msg.getContent() != null) {
                                    String token = msg.getContent().toString();
                                    fullContent.append(token);
                                    callback.onToken(token);
                                }
                            }
                        },
                        error -> {
                            log.error("AI流式请求失败", error);
                            callback.onError(error.getMessage());
                            latch.countDown();
                        },
                        () -> {
                            callback.onComplete(fullContent.toString());
                            latch.countDown();
                        }
                );

        try {
            if (!latch.await(5, TimeUnit.MINUTES)) {
                callback.onError("AI请求超时");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            callback.onError("请求中断");
        }
    }

    public String syncChat(List<ChatMessage> messages) {
        AtomicReference<String> result = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        streamChat(messages, new ChatStreamCallback() {
            @Override
            public void onToken(String token) {}

            @Override
            public void onComplete(String fullContent) {
                result.set(fullContent);
                latch.countDown();
            }

            @Override
            public void onError(String error) {
                log.error("AI同步请求失败: {}", error);
                latch.countDown();
            }
        });

        try {
            latch.await(5, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return result.get();
    }

    public interface ChatStreamCallback {
        void onToken(String token);
        void onComplete(String fullContent);
        void onError(String error);
    }

}
