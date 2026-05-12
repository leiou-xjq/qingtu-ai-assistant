package com.qingtu.agent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qingtu.agent.entity.po.SysNotification;
import org.apache.ibatis.annotations.Mapper;

/**
 * 系统消息通知Mapper接口
 * 
 * 对应数据库表：sys_notification
 * 
 * @author 青途智伴技术团队
 */
@Mapper
public interface SysNotificationMapper extends BaseMapper<SysNotification> {
}