package com.qingtu.agent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qingtu.agent.entity.po.ChatMessage;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ChatMessageMapper extends BaseMapper<ChatMessage> {
}
