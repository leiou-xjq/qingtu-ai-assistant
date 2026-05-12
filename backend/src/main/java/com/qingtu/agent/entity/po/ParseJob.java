package com.qingtu.agent.entity.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文档解析任务实体
 */
@Data
@TableName("parse_job")
public class ParseJob {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String fileName;

    private String fileUrl;

    private String fileType;

    private String status;  // PENDING / PROCESSING / COMPLETED / FAILED / CONFIRMED

    private Integer progress;  // 0-100

    private String result;  // JSON 格式的解析结果

    private String errorMessage;

    private String clarifyingQuestions;  // JSON 格式的澄清问题

    private String confirmedData;  // 用户确认后的数据

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    private LocalDateTime completedAt;
}
