package com.qingtu.agent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qingtu.agent.entity.po.CourseSchedule;
import org.apache.ibatis.annotations.Mapper;

/**
 * 课程表Mapper接口
 * 
 * 对应数据库表：course_schedule
 * 
 * @author 青途智伴技术团队
 */
@Mapper
public interface CourseScheduleMapper extends BaseMapper<CourseSchedule> {
}