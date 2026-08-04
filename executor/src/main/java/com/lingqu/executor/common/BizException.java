package com.lingqu.executor.common;

/**
 * 业务异常，code 同时作为 HTTP 状态码返回（401/403/404/429/500）。
 */
public class BizException extends RuntimeException {

    private final int code;

    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
