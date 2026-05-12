package com.qingtu.agent.exception;

import com.qingtu.agent.common.ResultCode;
import lombok.Getter;

/**
 * AI服务异常类
 * 
 * 功能说明：
 * - 用于封装AI服务调用过程中的异常
 * - 区分超时、额度、解析等不同类型的AI错误
 * 
 * @author 青途智伴技术团队
 */
@Getter
public class AIServiceException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * 错误码
     */
    private final int code;

    public AIServiceException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.code = resultCode.getCode();
    }

    public AIServiceException(ResultCode resultCode, String message) {
        super(message);
        this.code = resultCode.getCode();
    }

    public AIServiceException(int code, String message) {
        super(message);
        this.code = code;
    }

    public AIServiceException(int code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }
}