package com.qingtu.agent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qingtu.agent.entity.po.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户Mapper接口
 * 
 * 对应数据库表：user
 * 
 * @author 青途智伴技术团队
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
}