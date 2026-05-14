package com.qingtu.agent.tool.callback;

import com.qingtu.agent.tool.ToolExecutor;
import lombok.RequiredArgsConstructor;
import java.util.Map;

@RequiredArgsConstructor
public class ToolExecutorAdapter implements ToolCallback {
    private final ToolExecutor delegate;

    @Override
    public String getName() { return delegate.getName(); }
    @Override
    public String getDescription() { return delegate.getDescription(); }
    @Override
    public Map<String, Object> getInputSchema() { return Map.of(); }
    @Override
    public ToolCallbackResult execute(Map<String, Object> input) {
        try {
            var result = delegate.execute(input);
            return result.success()
                ? ToolCallbackResult.success(result.data())
                : ToolCallbackResult.failure(result.errorMessage());
        } catch (Exception e) {
            return ToolCallbackResult.failure(e.getMessage());
        }
    }
    @Override
    public String getCategory() { return delegate.getCategory(); }
    @Override
    public boolean isEnabled() { return delegate.isEnabled(); }
    @Override
    public long getTimeoutMs() { return delegate.getTimeoutMs(); }
}