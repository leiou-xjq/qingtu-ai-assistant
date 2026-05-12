package com.qingtu.agent.common;

import lombok.Getter;

/**
 * 响应码枚举类
 * 
 * 定义系统所有业务响应码，统一管理
 * 
 * @author 青途智伴技术团队
 */
@Getter
public enum ResultCode {

    SUCCESS(200, "操作成功"),
    FAIL(500, "操作失败"),
    VALIDATE_FAIL(400, "参数校验失败"),
    UNAUTHORIZED(401, "未登录或token已过期"),
    FORBIDDEN(403, "无权限访问"),
    NOT_FOUND(404, "资源不存在"),
    SERVER_ERROR(500, "服务器内部错误"),
    
    USER_NOT_FOUND(1001, "用户不存在"),
    USER_ALREADY_EXISTS(1002, "用户已存在"),
    PASSWORD_ERROR(1003, "密码错误"),
    PHONE_ALREADY_EXISTS(1004, "手机号已注册"),
    INVALID_TOKEN(1005, "无效的token"),
    
    HEALTH_RECORD_NOT_FOUND(2001, "健康档案不存在"),
    HEALTH_RECORD_EXISTS(2002, "健康档案已存在"),
    
    COURSE_NOT_FOUND(3001, "课程不存在"),
    COURSE_TIME_CONFLICT(3002, "课程时间冲突"),
    COURSE_IMPORT_ERROR(3003, "课程导入失败"),
    
    COST_RECORD_NOT_FOUND(4001, "消费记录不存在"),
    COST_IMPORT_ERROR(4002, "账单导入失败"),
    COST_CATEGORY_ERROR(4003, "消费分类错误"),
    
    DISH_NOT_FOUND(5001, "菜品不存在"),
    DISH_RECOMMEND_ERROR(5002, "推荐生成失败"),
    
    NOTE_NOT_FOUND(6001, "笔记不存在"),
    NOTE_GENERATE_ERROR(6002, "笔记生成失败"),
    NOTE_EXPORT_ERROR(6003, "笔记导出失败"),
    
    AI_SERVICE_ERROR(7001, "AI服务调用失败"),
    AI_TIMEOUT(7002, "AI响应超时"),
    AI_PARSE_ERROR(7003, "AI响应解析失败"),
    AI_QUOTA_EXCEEDED(7004, "AI调用额度已用完"),
    
    RAG_INDEX_ERROR(8001, "知识库索引失败"),
    RAG_SEARCH_ERROR(8002, "知识库检索失败"),
    RAG_CONTEXT_EMPTY(8003, "未找到相关知识上下文"),
    
    TASK_NOT_FOUND(9001, "任务不存在"),
    TASK_RUNNING(9002, "任务正在执行中"),
    TASK_DISABLED(9003, "任务已禁用"),
    TASK_LOCKED(9004, "任务正在被其他实例执行"),
    
    WEATHER_API_ERROR(10001, "天气API调用失败"),
    WEBHOOK_SEND_ERROR(10002, "消息推送失败"),
    FILE_UPLOAD_ERROR(10003, "文件上传失败"),
    FILE_TYPE_NOT_SUPPORT(10004, "不支持的文件类型"),
    CACHE_ERROR(10005, "缓存操作失败");

    private final int code;
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}