package com.qingtu.agent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qingtu.agent.entity.po.UserHealth;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户健康档案Mapper接口
 * 
 * 对应数据库表：user_health
 * 
 * @author 青途智伴技术团队
 */
@Mapper
public interface UserHealthMapper extends BaseMapper<UserHealth> {
}