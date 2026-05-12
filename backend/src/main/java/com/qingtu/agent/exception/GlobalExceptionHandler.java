package com.qingtu.agent.exception;

import com.qingtu.agent.common.CommonResult;
import com.qingtu.agent.common.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 全局异常处理器
 * 
 * 功能说明：
 * - 统一处理所有未捕获的异常
 * - 根据异常类型返回统一的错误响应
 * - 记录异常日志便于排查问题
 * - 支持参数校验、文件上传、AI服务等异常处理
 * 
 * @author 青途智伴技术团队
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理业务异常
     */
    @ExceptionHandler(BusinessException.class)
    public CommonResult<?> handleBusinessException(BusinessException e, HttpServletRequest request) {
        log.warn("业务异常 - URI: {}, 错误码: {}, 错误信息: {}", 
                request.getRequestURI(), e.getCode(), e.getMessage());
        return CommonResult.fail(e.getCode(), e.getMessage());
    }

    /**
     * 处理参数校验异常（@Valid注解）
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public CommonResult<?> handleValidationException(MethodArgumentNotValidException e, HttpServletRequest request) {
        List<FieldError> fieldErrors = e.getBindingResult().getFieldErrors();
        String errorMessage = fieldErrors.stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        
        log.warn("参数校验失败 - URI: {}, 错误信息: {}", request.getRequestURI(), errorMessage);
        return CommonResult.fail(ResultCode.VALIDATE_FAIL.getCode(), errorMessage);
    }

    /**
     * 处理参数绑定异常
     */
    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public CommonResult<?> handleBindException(BindException e, HttpServletRequest request) {
        List<FieldError> fieldErrors = e.getBindingResult().getFieldErrors();
        String errorMessage = fieldErrors.stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        
        log.warn("参数绑定失败 - URI: {}, 错误信息: {}", request.getRequestURI(), errorMessage);
        return CommonResult.fail(ResultCode.VALIDATE_FAIL.getCode(), errorMessage);
    }

    /**
     * 处理文件上传大小超限异常
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public CommonResult<?> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException e, HttpServletRequest request) {
        log.warn("文件上传超限 - URI: {}, 错误信息: {}", request.getRequestURI(), e.getMessage());
        return CommonResult.fail(ResultCode.FILE_UPLOAD_ERROR.getCode(), "上传文件大小超出限制，请上传小于10MB的文件");
    }

    /**
     * 处理空指针异常
     */
    @ExceptionHandler(NullPointerException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public CommonResult<?> handleNullPointerException(NullPointerException e, HttpServletRequest request) {
        log.error("空指针异常 - URI: {}", request.getRequestURI(), e);
        return CommonResult.fail(ResultCode.SERVER_ERROR.getCode(), "系统内部错误，请稍后重试");
    }

    /**
     * 处理数组越界异常
     */
    @ExceptionHandler(ArrayIndexOutOfBoundsException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public CommonResult<?> handleArrayIndexOutOfBoundsException(ArrayIndexOutOfBoundsException e, HttpServletRequest request) {
        log.error("数组越界 - URI: {}", request.getRequestURI(), e);
        return CommonResult.fail(ResultCode.SERVER_ERROR.getCode(), "系统内部错误，请稍后重试");
    }

    /**
     * 处理类型转换异常
     */
    @ExceptionHandler(ClassCastException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public CommonResult<?> handleClassCastException(ClassCastException e, HttpServletRequest request) {
        log.error("类型转换异常 - URI: {}", request.getRequestURI(), e);
        return CommonResult.fail(ResultCode.SERVER_ERROR.getCode(), "系统内部错误，请稍后重试");
    }

    /**
     * 处理非法参数异常
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public CommonResult<?> handleIllegalArgumentException(IllegalArgumentException e, HttpServletRequest request) {
        log.warn("非法参数异常 - URI: {}, 错误信息: {}", request.getRequestURI(), e.getMessage());
        return CommonResult.fail(ResultCode.VALIDATE_FAIL.getCode(), e.getMessage());
    }

    /**
     * 处理AI服务异常
     */
    @ExceptionHandler(AIServiceException.class)
    public CommonResult<?> handleAIServiceException(AIServiceException e, HttpServletRequest request) {
        log.error("AI服务异常 - URI: {}, 错误码: {}, 错误信息: {}", 
                request.getRequestURI(), e.getCode(), e.getMessage(), e);
        return CommonResult.fail(e.getCode(), e.getMessage());
    }

    /**
     * 处理RAG服务异常
     */
    @ExceptionHandler(RAGServiceException.class)
    public CommonResult<?> handleRAGServiceException(RAGServiceException e, HttpServletRequest request) {
        log.error("RAG服务异常 - URI: {}, 错误码: {}, 错误信息: {}", 
                request.getRequestURI(), e.getCode(), e.getMessage(), e);
        return CommonResult.fail(e.getCode(), e.getMessage());
    }

    /**
     * 处理所有未捕获的异常
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public CommonResult<?> handleException(Exception e, HttpServletRequest request) {
        log.error("未知异常 - URI: {}", request.getRequestURI(), e);
        return CommonResult.fail(ResultCode.SERVER_ERROR.getCode(), "系统发生未知错误，请稍后重试");
    }
}