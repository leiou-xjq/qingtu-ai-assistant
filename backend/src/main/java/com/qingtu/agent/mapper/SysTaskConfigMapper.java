package com.qingtu.agent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qingtu.agent.entity.po.SysTaskConfig;
import org.apache.ibatis.annotations.Mapper;

/**
 * 定时任务配置Mapper接口
 * 
 * 对应数据库表：sys_task_config
 * 
 * @author 青途智伴技术团队
 */
@Mapper
public interface SysTaskConfigMapper extends BaseMapper<SysTaskConfig> {
}