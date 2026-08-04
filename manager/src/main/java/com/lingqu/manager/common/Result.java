package com.lingqu.manager.common;

/**
 * 统一响应体。code = 0 表示成功，非 0 为业务错误码。
 */
public class Result<T> {

    public static final int CODE_OK = 0;

    private int code;
    private String message;
    private T data;

    public Result() {
    }

    public Result(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> Result<T> ok() {
        return new Result<>(CODE_OK, "ok", null);
    }

    public static <T> Result<T> ok(T data) {
        return new Result<>(CODE_OK, "ok", data);
    }

    public static <T> Result<T> error(int code, String message) {
        return new Result<>(code, message, null);
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
