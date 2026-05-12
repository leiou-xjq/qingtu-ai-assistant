package com.qingtu.agent.entity.dto;

import lombok.Data;
import java.io.Serializable;

/**
 * AI问答DTO
 *
 * @author 青途智伴技术团队
 */
@Data
public class AskDTO implements Serializable {

    private String question;

    private Long sessionId;

    private String category;

    private Integer topK;
}