package com.qingtu.agent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qingtu.agent.entity.po.Notes;
import org.apache.ibatis.annotations.Mapper;

/**
 * 笔记 Mapper
 */
@Mapper
public interface NotesMapper extends BaseMapper<Notes> {
}