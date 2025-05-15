package com.biggerboy.springaidemo.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.biggerboy.springaidemo.domain.ConversationMessage;
import org.springframework.ai.chat.messages.Message;

import java.util.List;

/**
 * @author BiggerBoy
 * @description 针对表【message】的数据库操作Service
 * @createDate 2025-05-13 11:07:18
 */
public interface ConversationMessageService extends IService<ConversationMessage> {

    List<Message> loadMessages(String conversationId);

    void storeMessage(String conversationId, List<Message> updatedMessages);

    List<ConversationMessage> selectByRequestId(String conversationId);
}
