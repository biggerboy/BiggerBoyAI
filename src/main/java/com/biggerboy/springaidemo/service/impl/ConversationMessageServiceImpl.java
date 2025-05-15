package com.biggerboy.springaidemo.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.biggerboy.springaidemo.domain.ConversationMessage;
import com.biggerboy.springaidemo.mapper.ConversationMessageMapper;
import com.biggerboy.springaidemo.service.ConversationMessageService;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author BiggerBoy
 * @description 针对表【message】的数据库操作Service实现
 * @createDate 2025-05-13 11:07:18
 */
@Service
public class ConversationMessageServiceImpl extends ServiceImpl<ConversationMessageMapper, ConversationMessage>
        implements ConversationMessageService {
    @Autowired
    private ConversationMessageMapper conversationMessageMapper;

    @Override
    public List<Message> loadMessages(String conversationId) {
        List<ConversationMessage> messages = conversationMessageMapper.selectByConversationId(conversationId);
        List<Message> list = new ArrayList<>();
        messages.forEach(message -> {
            if ("user".equals(message.getRole())) {
                Message userMessage = new UserMessage(message.getContent());
                list.add(userMessage);
            }
            if ("assistant".equals(message.getRole())) {
                Message assistantMessage = new AssistantMessage(message.getContent());
                list.add(assistantMessage);
            }
        });
        return list;
    }

    @Override
    public void storeMessage(String conversationId, List<Message> updatedMessages) {
        if (CollectionUtils.isEmpty(updatedMessages)) {
            return;
        }
        List<ConversationMessage> messages = new ArrayList<>();
        updatedMessages.forEach(message -> {
            ConversationMessage conversationMessage = new ConversationMessage();
            conversationMessage.setConversationId(conversationId);
            conversationMessage.setContent(message.getText());
            conversationMessage.setRole(message.getMessageType().getValue());
            conversationMessage.setCreatedAt(new Date());
            messages.add(conversationMessage);
        });
        if (CollectionUtils.isEmpty(messages)) {
            return;
        }
        conversationMessageMapper.batchInsert(messages);
    }

    @Override
    public List<ConversationMessage> selectByRequestId(String conversationId) {
        return conversationMessageMapper.selectByConversationId(conversationId);
    }
}




