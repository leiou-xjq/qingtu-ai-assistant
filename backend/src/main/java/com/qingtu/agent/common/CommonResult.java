package com.qingtu.agent.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 统一响应结果封装类
 * 
 * 功能说明：
 * - 所有API接口统一返回此格式
 * - 包含状态码、消息、数据三部分
 * - 支持链式调用
 * 
 * 使用示例：
 * - 成功：CommonResult.success(data)
 * - 失败：CommonResult.fail(Code.FAIL, "操作失败")
 * 
 * @param <T> 数据类型
 * @author 青途智伴技术团队
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommonResult<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 状态码
     * 200: 成功
     * 400: 请求参数错误
     * 401: 未登录/token失效
     * 403: 无权限
     * 404: 资源不存在
     * 500: 服务器内部错误
     */
    private int code;

    /**
     * 响应消息
     */
    private String msg;

    /**
     * 响应数据
     */
    private T data;

    /**
     * 请求ID（用于日志追踪）
     */
    private String requestId;

    /**
     * 时间戳
     */
    private long timestamp;

    /**
     * 创建成功响应（带数据）
     */
    public static <T> CommonResult<T> success(T data) {
        return new CommonResult<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), data, null, System.currentTimeMillis());
    }

    /**
     * 创建成功响应（带消息）
     */
    public static <T> CommonResult<T> success(String message, T data) {
        return new CommonResult<>(ResultCode.SUCCESS.getCode(), message, data, null, System.currentTimeMillis());
    }

    /**
     * 创建成功响应（无数据）
     */
    public static <T> CommonResult<T> success() {
        return success(null);
    }

    /**
     * 创建失败响应
     */
    public static <T> CommonResult<T> fail(ResultCode resultCode) {
        return new CommonResult<>(resultCode.getCode(), resultCode.getMessage(), null, null, System.currentTimeMillis());
    }

    /**
     * 创建失败响应（自定义消息）
     */
    public static <T> CommonResult<T> fail(ResultCode resultCode, String message) {
        return new CommonResult<>(resultCode.getCode(), message, null, null, System.currentTimeMillis());
    }

    /**
     * 创建失败响应（自定义码和消息）
     */
    public static <T> CommonResult<T> fail(int code, String message) {
        return new CommonResult<>(code, message, null, null, System.currentTimeMillis());
    }

    public static <T> CommonResult<T> fail(String message) {
        return new CommonResult<>(ResultCode.FAIL.getCode(), message, null, null, System.currentTimeMillis());
    }

    /**
     * 判断是否成功
     */
    public boolean isSuccess() {
        return this.code == ResultCode.SUCCESS.getCode();
    }
}