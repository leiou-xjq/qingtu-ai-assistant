package com.qingtu.agent.mcp.server.auth;

import java.lang.annotation.*;

/**
 * 工具权限注解 - 标注在 MCP 工具方法上
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequireToolPermission {
    ToolPermission value() default ToolPermission.USER;
}
