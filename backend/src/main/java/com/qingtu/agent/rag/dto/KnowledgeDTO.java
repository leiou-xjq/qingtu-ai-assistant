package com.qingtu.agent.rag.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeDTO {

    private String school;

    private String schoolName;

    private String category;

    private String title;

    private String content;

    private String source;

    private String tags;

    private LocalDateTime crawlTime;
}