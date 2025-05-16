package com.biggerboy.springaidemo.service;

import com.biggerboy.springaidemo.domain.Conversation;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
* @author BiggerBoy
* @description 针对表【conversation】的数据库操作Service
* @createDate 2025-05-13 11:07:18
*/
public interface ConversationService extends IService<Conversation> {

    Conversation createConversation(String conversationId, String title );

    /**
     * 查询对话列表
     * @return 对话列表
     */
    List<Conversation> getConversationList();
}
