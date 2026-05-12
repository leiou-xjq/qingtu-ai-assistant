package com.qingtu.agent.exception;

import com.qingtu.agent.common.ResultCode;
import lombok.Getter;

/**
 * RAG服务异常类
 * 
 * 功能说明：
 * - 用于封装RAG知识库服务调用过程中的异常
 * - 区分索引、检索、上下文为空等不同类型的错误
 * 
 * @author 青途智伴技术团队
 */
@Getter
public class RAGServiceException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * 错误码
     */
    private final int code;

    public RAGServiceException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.code = resultCode.getCode();
    }

    public RAGServiceException(ResultCode resultCode, String message) {
        super(message);
        this.code = resultCode.getCode();
    }

    public RAGServiceException(int code, String message) {
        super(message);
        this.code = code;
    }

    public RAGServiceException(int code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }
}