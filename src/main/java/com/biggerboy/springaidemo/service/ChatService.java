package com.biggerboy.springaidemo.service;

import com.biggerboy.springaidemo.ai.MysqlChatMemoryRepository;
import com.biggerboy.springaidemo.domain.Conversation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.zhipuai.ZhiPuAiChatModel;
import org.springframework.ai.zhipuai.ZhiPuAiChatOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
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
    @Autowired
    private MysqlChatMemoryRepository mysqlChatMemoryRepository;
    @Autowired
    private ConversationService conversationService;

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

    public SseEmitter generateStreamWithLocalV2(String userText, Boolean local, String conversationId) {
        SseEmitter emitter = new SseEmitter();
        Conversation conversation = conversationService.createConversation(conversationId);
        ZhiPuAiChatOptions chatOptions = ZhiPuAiChatOptions.builder()
                .temperature(1.0)
                .build();


        List<Advisor> advisors = new ArrayList<>();
        if (local) {
            //使用本地向量存储
            QuestionAnswerAdvisor questionAnswerAdvisor = new QuestionAnswerAdvisor(vectorStore);
            advisors.add(questionAnswerAdvisor);
        }
        //获取历史对话
        MessageWindowChatMemory messageWindowChatMemory = MessageWindowChatMemory.builder().chatMemoryRepository(mysqlChatMemoryRepository).build();
        MessageChatMemoryAdvisor messageChatMemoryAdvisor = new MessageChatMemoryAdvisor(messageWindowChatMemory);
        advisors.add(messageChatMemoryAdvisor);

        ChatClient.Builder builder = ChatClient.builder(chatModel);
        //自定义conversationId 用于记录多次对话
        builder.defaultAdvisors(advisorSpec -> advisorSpec.params(Map.of(AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY, conversationId)));
        ChatClient chatClient = builder
                .build();

        ChatClient.ChatClientRequestSpec chatClientRequestSpec = chatClient.prompt();

        chatClientRequestSpec
                .system("你是一个智能助手，用户会问你问题。如果用户给定的知识库没有相关信息，你需要根据你自己掌握的知识回答，并且不需要在回复的内容中指出这一点。")
                .advisors(advisors)
                .user(userText)
                .options(chatOptions)
                .stream()
                .chatResponse()
                .subscribe(
                        chunk -> {
                            try {
                                String content = chunk.getResult().getOutput().getText();
                                // 发送消息到客户端
                                // 注意这里，我们直接发送 JSON 字符串，让 SseEmitter 自动添加 data: 前缀
                                emitter.send(Map.of("content", content));
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
                        () -> emitter.complete());
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
