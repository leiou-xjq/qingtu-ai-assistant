package com.qingtu.agent.entity.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 笔记实体
 */
@Data
@TableName("notes")
public class Notes {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String title;

    private String content;

    private String noteType;

    private Long courseId;

    @TableLogic
    private Integer deleted = 0;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}