package com.qingtu.agent.entity.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("async_task")
public class AsyncTask implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String question;

    private String sessionId;

    private String status;

    private String answer;

    private String errorMessage;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}