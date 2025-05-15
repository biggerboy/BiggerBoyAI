package com.biggerboy.springaidemo.controller;

import com.biggerboy.springaidemo.domain.ConversationMessage;
import com.biggerboy.springaidemo.service.ConversationMessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * @Author 公众号：BiggerBoy
 * @date 2025/5/13  11:05
 */
@RestController
@RequestMapping("/conversationMessage")
public class ConversationMessageController {
    @Autowired
    private ConversationMessageService conversationMessageService;

    /**
     * 获取对话列表
     *
     * @return 对话列表
     */
    @GetMapping("/list/{conversationId}")
    public List<ConversationMessage> getConversationList(@PathVariable @NotNull String conversationId) {
        return conversationMessageService.selectByRequestId(conversationId);
    }
}
