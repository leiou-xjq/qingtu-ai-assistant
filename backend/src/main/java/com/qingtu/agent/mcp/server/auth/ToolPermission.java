package com.qingtu.agent.mcp.server.auth;

/**
 * 工具权限等级枚举
 */
public enum ToolPermission {
    PUBLIC,      // 公开：所有用户可用
    USER,        // 用户：已登录用户
    PREMIUM,     // 高级：VIP 用户
    ADMIN        // 管理员
}
