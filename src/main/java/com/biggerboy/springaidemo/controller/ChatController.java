package com.biggerboy.springaidemo.controller;

import com.biggerboy.springaidemo.service.ChatService;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.zhipuai.ZhiPuAiChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @Author 公众号：BiggerBoy
 * @date 2025/5/8  11:05
 */
@RestController
public class ChatController {


    @Autowired
    private ChatService chatService;

    private final ZhiPuAiChatModel chatModel;

    @Autowired
    public ChatController(ZhiPuAiChatModel chatModel) {
        this.chatModel = chatModel;
    }

    /**
     * 生成回复
     *
     * @param message
     * @return
     */
    @GetMapping("/ai/generate")
    public Map generate(@RequestParam(value = "message", defaultValue = "给我讲个笑话") String message) {
        // 如果没有匹配的答案，调用AI模型生成回复
        return Map.of("generation", this.chatModel.call(message));
    }

    /**
     * 流式生成回复
     *
     * @param message
     * @param local
     * @return
     */
    @GetMapping("/ai/v1/generateStream")
    public SseEmitter generateStream(@RequestParam @NotNull String message,
                                     @RequestParam Boolean local) {
        if (Boolean.FALSE.equals(local)) {
            return chatService.generateStream(message);
        }
        return chatService.generateStreamWithLocal(message);
    }

    @GetMapping("/ai/v2/generateStream")
    public SseEmitter generateStreamV2(@RequestParam @NotNull String message,
                                       @RequestParam Boolean local,
                                       @RequestParam @NotNull String requestId) {
        return chatService.generateStreamWithLocalV2(message, local, requestId);
    }


}