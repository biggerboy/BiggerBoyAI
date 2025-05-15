package com.biggerboy.springaidemo.controller;

import com.biggerboy.springaidemo.domain.Conversation;
import com.biggerboy.springaidemo.service.ConversationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
/**
 * @Author 公众号：BiggerBoy
 * @date 2025/5/13  11:05
 */
@RestController
@RequestMapping("/conversations")
public class ConversationController {

    @Autowired
    private ConversationService conversationService;

    /**
     * 获取对话列表
     * @return 对话列表
     */
    @GetMapping
    public List<Conversation> getConversationList() {
        return conversationService.getConversationList();
    }
}
