package com.dhxz.search.exception;

import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * @author 作者 cyx
 * @version 创建时间：
 */
@Slf4j
@RestControllerAdvice("com.dhxz.search.web")
public class ServiceExceptionHandleAction {

    @ExceptionHandler
    public Map<String, Object> handleAndReturnData(Exception ex) {
        log.error("异常信息为：", ex);
        Map<String, Object> data = new HashMap<>();
        if (ex instanceof ServiceException) {
            ServiceException e = (ServiceException) ex;
            data.put("code", e.exceptionEnum().getErrorCode());
            data.put("msg", e.exceptionEnum().getErrorMessage());
        } else {
            data.put("code", 500);
            data.put("msg", "系统繁忙请稍后重试!");
        }
        data.put("success", false);
        data.put("exception", ex.getClass().getSimpleName());
        return data;
    }

}
