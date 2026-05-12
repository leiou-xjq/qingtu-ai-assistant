package com.qingtu.agent.mcp.server;

import com.qingtu.agent.common.CommonResult;
import com.qingtu.agent.mcp.server.config.McpServerConfig;
import com.qingtu.agent.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * MCP Server REST API
 */
@RestController
@RequestMapping("/mcp")
@RequiredArgsConstructor
public class McpController {

    private final QingTuMcpServer mcpServer;
    private final McpServerConfig config;
    private final JwtUtil jwtUtil;

    @PostMapping("/call")
    public CommonResult<?> call(HttpServletRequest request, @RequestBody McpRequest mcpRequest) {
        mcpRequest.setRequestId(UUID.randomUUID().toString());
        mcpRequest.setUserId(getUserIdFromRequest(request));
        mcpRequest.setTimestamp(System.currentTimeMillis());

        McpResponse response = mcpServer.execute(mcpRequest);

        if (response.isSuccess()) {
            return CommonResult.success(response.getData());
        } else {
            return CommonResult.fail(response.getErrorCode() + ": " + response.getErrorMessage());
        }
    }

    @GetMapping("/tools")
    public CommonResult<?> getTools() {
        return CommonResult.success(mcpServer.getAvailableTools());
    }

    @GetMapping("/tools/definitions")
    public CommonResult<?> getToolDefinitions() {
        return CommonResult.success(mcpServer.getToolDefinitions());
    }

    @GetMapping("/health")
    public CommonResult<?> health() {
        return CommonResult.success(Map.of(
                "status", "UP",
                "version", config.getVersion(),
                "enabled", config.isEnabled()
        ));
    }

    private Long getUserIdFromRequest(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null) return null;
        String token = jwtUtil.extractToken(authHeader);
        return jwtUtil.getUserId(token);
    }
}
