package com.qingtu.agent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qingtu.agent.entity.po.CourseKeyPoint;
import org.apache.ibatis.annotations.Mapper;

/**
 * 课程AI笔记Mapper接口
 * 
 * 对应数据库表：course_key_point
 * 
 * @author 青途智伴技术团队
 */
@Mapper
public interface CourseKeyPointMapper extends BaseMapper<CourseKeyPoint> {
}