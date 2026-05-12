package com.qingtu.agent.entity.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("chat_message")
public class ChatMessage implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long sessionId;

    private String role;

    private String content;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
