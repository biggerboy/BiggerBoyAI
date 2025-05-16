package com.biggerboy.springaidemo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.biggerboy.springaidemo.domain.Conversation;
import com.biggerboy.springaidemo.mapper.ConversationMapper;
import com.biggerboy.springaidemo.service.ConversationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * @author BiggerBoy
 * @description 针对表【conversation】的数据库操作Service实现
 * @createDate 2025-05-13 11:07:18
 */
@Service
public class ConversationServiceImpl extends ServiceImpl<ConversationMapper, Conversation>
        implements ConversationService {
    @Autowired
    private ConversationMapper conversationMapper;

    @Override
    public Conversation createConversation(String conversationId, String title) {
        Conversation conversation1 = conversationMapper.selectOne(new LambdaQueryWrapper<Conversation>().eq(Conversation::getConversationId, conversationId));
        String finalTitle = title;
        conversation1 = Optional.ofNullable(conversation1).orElseGet(() -> {
            Conversation conversation = new Conversation();
            conversation.setConversationId(conversationId);
            conversation.setTitle(finalTitle);
            conversation.setCreatedAt(new Date());
            conversationMapper.insert(conversation);
            return conversation;
        });
        return conversation1;
    }

    @Override
    public List<Conversation> getConversationList() {
        return conversationMapper.selectList(new LambdaQueryWrapper<Conversation>().orderByDesc(Conversation::getCreatedAt));
    }
}




