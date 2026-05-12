package com.qingtu.agent.entity.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("conversation_log")
public class ConversationLog implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String traceId;
    private Long userId;
    private String sessionId;
    private String role;
    private String content;
    private String intent;
    private String toolUsed;
    private Integer tokenUsed;
    private Integer latencyMs;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}