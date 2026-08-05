package com.lingqu.manager.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;

/**
 * 全局异常处理：业务异常返回对应错误码，参数校验错误返回 400，其余返回 500。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BizException.class)
    public Result<Void> handleBiz(BizException e) {
        return Result.error(e.getCode(), e.getMessage());
    }

    /** 请求方法不支持（如用 GET 访问 POST 接口）：返回 405，不打 ERROR 日志 */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public Result<Void> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        log.warn("请求方法不支持：{} {}", e.getMethod(), "（仅支持 "
                + String.join("/", e.getSupportedMethods() == null ? new String[0] : e.getSupportedMethods()) + "）");
        return Result.error(405, "请求方法不支持：" + e.getMethod());
    }

    /** 路径不存在：返回 404 */
    @ExceptionHandler(NoHandlerFoundException.class)
    public Result<Void> handleNotFound(NoHandlerFoundException e) {
        return Result.error(404, "接口不存在");
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public Result<Void> handleValidation(BindException e) {
        FieldError fe = e.getBindingResult().getFieldError();
        String msg = fe == null ? "参数校验失败" : fe.getField() + " " + fe.getDefaultMessage();
        return Result.error(400, msg);
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> handleOther(Exception e) {
        log.error("unexpected error", e);
        return Result.error(500, e.getMessage() == null ? "服务器内部错误" : e.getMessage());
    }
}
