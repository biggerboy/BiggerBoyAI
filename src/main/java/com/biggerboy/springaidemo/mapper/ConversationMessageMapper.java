package com.biggerboy.springaidemo.mapper;

import com.biggerboy.springaidemo.domain.ConversationMessage;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import java.util.List;

/**
 * @author BiggerBoy
 * @description 针对表【message】的数据库操作Mapper
 * @createDate 2025-05-13 11:07:18
 * @Entity generator.domain.Message
 */
public interface ConversationMessageMapper extends BaseMapper<ConversationMessage> {

    List<ConversationMessage> selectByConversationId(String conversationId);

    void batchInsert(List<ConversationMessage> messages);
}




