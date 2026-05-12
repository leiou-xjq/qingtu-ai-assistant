package com.qingtu.agent.exception;

import com.qingtu.agent.common.ResultCode;
import lombok.Getter;

/**
 * 业务异常类
 * 
 * 功能说明：
 * - 用于抛出业务逻辑相关的异常
 * - 包含错误码和错误信息
 * - 支持自定义错误码和消息
 * 
 * 使用示例：
 * - throw new BusinessException(ResultCode.USER_NOT_FOUND);
 * - throw new BusinessException(ResultCode.FAIL, "自定义错误信息");
 * 
 * @author 青途智伴技术团队
 */
@Getter
public class BusinessException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * 错误码
     */
    private final int code;

    /**
     * 错误信息
     */
    private final String message;

    public BusinessException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.code = resultCode.getCode();
        this.message = resultCode.getMessage();
    }

    public BusinessException(ResultCode resultCode, String message) {
        super(message);
        this.code = resultCode.getCode();
        this.message = message;
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }

    public BusinessException(int code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.message = message;
    }
}