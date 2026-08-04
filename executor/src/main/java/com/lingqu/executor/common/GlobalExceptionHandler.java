package com.lingqu.executor.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理：业务异常直接映射为对应 HTTP 状态码（业务 API 语义），
 * 其余异常返回 500。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BizException.class)
    public ResponseEntity<Result<Void>> handleBiz(BizException e) {
        int status = e.getCode() >= 400 && e.getCode() < 600 ? e.getCode() : 500;
        return ResponseEntity.status(status).body(Result.error(e.getCode(), e.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleOther(Exception e) {
        log.error("unexpected error", e);
        String msg = e.getMessage() == null ? "服务器内部错误" : e.getMessage();
        return ResponseEntity.status(500).body(Result.error(500, msg));
    }
}
