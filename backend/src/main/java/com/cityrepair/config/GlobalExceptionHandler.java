package com.cityrepair.config;

import com.cityrepair.common.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(RuntimeException.class)
    public ApiResponse<Void> handleRuntime(RuntimeException e) {
        String msg = e.getMessage();
        if (msg != null && msg.startsWith("403:")) {
            return ApiResponse.error(403, msg.substring(4));
        }
        log.error("Unexpected error", e);
        return ApiResponse.error(500, "服务器内部错误");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResponse<Void> handleValidation(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b).orElse("参数校验失败");
        return ApiResponse.error(400, msg);
    }
}
