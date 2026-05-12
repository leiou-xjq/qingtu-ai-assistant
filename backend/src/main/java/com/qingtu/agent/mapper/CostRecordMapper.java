package com.qingtu.agent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qingtu.agent.entity.po.CostRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 消费记录Mapper接口
 * 
 * 对应数据库表：cost_record
 * 
 * @author 青途智伴技术团队
 */
@Mapper
public interface CostRecordMapper extends BaseMapper<CostRecord> {
}