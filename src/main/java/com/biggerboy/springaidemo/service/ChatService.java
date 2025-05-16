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
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.content.Content;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.zhipuai.ZhiPuAiChatModel;
import org.springframework.ai.zhipuai.ZhiPuAiChatOptions;
import org.springframework.ai.zhipuai.api.ZhiPuAiApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

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
    @Autowired
    private ConversationMessageService conversationMessageService;

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
                        emitter::complete);
        return emitter;
    }

    public SseEmitter generateStreamWithLocalV2(String userText, Boolean local, String conversationId) {
        SseEmitter emitter = new SseEmitter();

        List<ZhiPuAiApi.FunctionTool> tools = new ArrayList<>();
        tools.add(new ZhiPuAiApi.FunctionTool(ZhiPuAiApi.FunctionTool.Type.FUNCTION, new ZhiPuAiApi.FunctionTool.Function("web_search",
                "web_search",
                "{\"enable\":\"True\"}")));
        ZhiPuAiChatOptions chatOptions = ZhiPuAiChatOptions.builder()
                .temperature(1.0)
//                .tools(tools)
                .build();


        List<Advisor> advisors = new ArrayList<>();
        if (local) {
            //使用本地向量存储
            PromptTemplate promptTemplate = new PromptTemplate("""
                    下面是上下文信息，被---------------------包裹

                    ---------------------
                    {question_answer_context}
                    ---------------------

                    优先根据上下文回复用户，如果上下文中没有相关内容，不用告知用户你的回答是基于上下文信息还是你自己的知识库，直接根据你所掌握的知识进行回复。
                    """);

            QuestionAnswerAdvisor questionAnswerAdvisor = QuestionAnswerAdvisor.builder(vectorStore).promptTemplate(promptTemplate).build();
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
//                .system("你是一个智能助手，用户会问你问题。如果用户给定的知识库没有相关信息，你需要根据你自己掌握的知识回答，并且不需要在回复的内容中指出这一点。")
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
                        () -> {
                            if (conversationService.lambdaQuery().eq(Conversation::getConversationId, conversationId).one() == null) {
                                List<Message> messages = conversationMessageService.loadMessages(conversationId);
                                String title = generateConversationTitle(messages);
                                Conversation conversation = conversationService.createConversation(conversationId, title);
                            }
                            emitter.complete();
                        });
        return emitter;
    }

    /**
     * 生成对话标题
     *
     * @param messages 对话消息列表
     * @return 生成的对话标题
     */
    private String generateConversationTitle(List<Message> messages) {
        // 将所有消息拼接成一个字符串
        String allMessages = messages.stream().map(Content::getText).collect(Collectors.joining(" "));

        // 这里可以添加更复杂的 NLP 处理逻辑，例如使用智普大模型生成标题
        // 示例调用智普大模型生成标题
        List<Message> titleMessages = new ArrayList<>();
        titleMessages.add(new SystemMessage("根据以下对话内容生成一个简洁的标题，不超过30个字"));
        titleMessages.add(new UserMessage(allMessages));

        String text = this.chatModel.call(new Prompt(titleMessages)).getResult().getOutput().getText();
        text = text.substring(0, 30);
        return text;
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
                                emitter::complete);

            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        });
        return emitter;
    }
}
