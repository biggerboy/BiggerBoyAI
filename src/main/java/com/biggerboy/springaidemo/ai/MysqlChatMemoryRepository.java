package com.biggerboy.springaidemo.ai;

import com.biggerboy.springaidemo.domain.Conversation;
import com.biggerboy.springaidemo.service.ConversationMessageService;
import com.biggerboy.springaidemo.service.ConversationService;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * MySQL实现的聊天内存存储库
 * @author BiggerBoy
 */
@Service
public class MysqlChatMemoryRepository implements ChatMemoryRepository {

    @Autowired
    private ConversationService conversationService;
    @Autowired
    private ConversationMessageService conversationMessageService;

    @Override
    public List<String> findConversationIds() {
        return conversationService.list()
                .stream()
                .map(Conversation::getConversationId)
                .toList();
    }

    @Override
    public List<Message> findByConversationId(String conversationId) {
        return conversationMessageService.loadMessages(conversationId);
    }

    @Override
    public void saveAll(String conversationId, List<Message> messages) {
        List<Message> list = conversationMessageService.loadMessages(conversationId);
        List<Message> messageList = messages.stream().filter(p -> !list.contains(p)).collect(Collectors.toList());
        conversationMessageService.storeMessage(conversationId, messageList);
    }

    @Override
    public void deleteByConversationId(String conversationId) {

    }
}
