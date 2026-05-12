package com.qingtu.agent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qingtu.agent.entity.po.CanteenDish;
import org.apache.ibatis.annotations.Mapper;

/**
 * 食堂菜品Mapper接口
 * 
 * 对应数据库表：canteen_dish
 * 
 * @author 青途智伴技术团队
 */
@Mapper
public interface CanteenDishMapper extends BaseMapper<CanteenDish> {
}