package com.biggerboy.springaidemo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.zhipuai.ZhiPuAiChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * @author BiggerBoy
 * @date 2025/5/7
 */
@Service
public class ChatService {
    final Logger logger = LoggerFactory.getLogger(VStoreService.class);
    @Autowired
    private VectorStore vectorStore;
    @Autowired
    private ZhiPuAiChatModel chatModel;


    /**
     * 处理用户输入并通过服务器发送事件（SSE）返回聊天模型的流式响应。
     * 使用 {@link QuestionAnswerAdvisor} 结合向量存储来提供更准确的回答。
     *
     * @param userText 用户输入的文本
     * @return 用于发送 SSE 事件的 {@link SseEmitter} 对象
     */
    public SseEmitter generateStreamWithLocal(String userText) {
        SseEmitter emitter = new SseEmitter();
        ChatClient.builder(chatModel)
                .build().prompt()
                .advisors(new QuestionAnswerAdvisor(vectorStore))
                .user(userText)
                .stream()
                .chatResponse()
                .subscribe(
                        chunk -> {
                            try {
                                String content = chunk.getResult().getOutput().getText();
                                if (StringUtils.hasText(content)) {
                                    // 发送消息到客户端
                                    // 注意这里，我们直接发送 JSON 字符串，让 SseEmitter 自动添加 data: 前缀
                                    emitter.send(Map.of("content", content));
                                }
                            } catch (IOException e) {
                                emitter.completeWithError(e);
                            }
                        },
                        error -> {
                            try {
                                emitter.send(Map.of("content", "发生错误: " + error.getMessage()));
                            } catch (IOException e) {
                                emitter.completeWithError(e);
                            } finally {
                                emitter.complete();
                            }
                        },
                        emitter::complete);
        return emitter;
    }

    /**
     * 异步处理用户输入并通过服务器发送事件（SSE）返回聊天模型的流式响应。
     *
     * @param userText 用户输入的文本
     * @return 用于发送 SSE 事件的 {@link SseEmitter} 对象
     */
    public SseEmitter generateStream(String userText) {
        SseEmitter emitter = new SseEmitter();
        CompletableFuture.runAsync(() -> {
            try {
                // 创建消息
                var prompt = new Prompt(new UserMessage(userText));
                // 使用 stream 方法获取流式响应
                chatModel.stream(prompt)
                        .subscribe(
                                chunk -> {
                                    try {
                                        String content = chunk.getResult().getOutput().getText();
                                        if (StringUtils.hasText(content)) {
                                            // 发送消息到客户端
                                            // 注意这里，我们直接发送 JSON 字符串，让 SseEmitter 自动添加 data: 前缀
                                            emitter.send(Map.of("content", content));
                                        }
                                    } catch (IOException e) {
                                        emitter.completeWithError(e);
                                    }
                                },
                                error -> {
                                    try {
                                        emitter.send(Map.of("content", "发生错误: " + error.getMessage()));
                                    } catch (IOException e) {
                                        emitter.completeWithError(e);
                                    } finally {
                                        emitter.complete();
                                    }
                                },
                                emitter::complete);

            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        });
        return emitter;
    }
}
