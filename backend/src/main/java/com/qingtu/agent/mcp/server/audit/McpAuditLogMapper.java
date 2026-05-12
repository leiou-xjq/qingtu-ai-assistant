package com.qingtu.agent.mcp.server.audit;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * MCP 审计日志 Mapper
 */
@Mapper
public interface McpAuditLogMapper extends BaseMapper<McpAuditLog> {
}
