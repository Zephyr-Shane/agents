package com.opencode.agents.common;

import lombok.Getter;

@Getter
public enum ErrorCode {

    SUCCESS(0, "ok"),
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未登录或token已过期"),
    FORBIDDEN(403, "无权限访问"),
    NOT_FOUND(404, "资源不存在"),
    BUSINESS_ERROR(1000, "业务异常"),
    AI_REQUEST_ERROR(1001, "AI请求失败"),
    AGENT_CREATE_ERROR(1002, "智能体创建失败"),
    CONVERSATION_NOT_FOUND(1003, "会话不存在"),
    SYSTEM_ERROR(5000, "系统异常");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

}
