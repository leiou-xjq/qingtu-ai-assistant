package com.qingtu.agent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qingtu.agent.entity.po.ParseJob;
import org.apache.ibatis.annotations.Mapper;

/**
 * 解析任务 Mapper
 */
@Mapper
public interface ParseJobMapper extends BaseMapper<ParseJob> {
}
