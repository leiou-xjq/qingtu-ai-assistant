package com.qingtu.agent.tool.callback;

import lombok.Data;

@Data
public class ToolCallbackResult {
    private boolean success;
    private Object data;
    private String errorMessage;

    public static ToolCallbackResult success(Object data) {
        ToolCallbackResult result = new ToolCallbackResult();
        result.setSuccess(true);
        result.setData(data);
        return result;
    }

    public static ToolCallbackResult failure(String errorMessage) {
        ToolCallbackResult result = new ToolCallbackResult();
        result.setSuccess(false);
        result.setErrorMessage(errorMessage);
        return result;
    }
}