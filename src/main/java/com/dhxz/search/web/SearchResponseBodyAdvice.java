package com.dhxz.search.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * @author berniewu
 * @className ResponseBodyAdvice
 * @date 2019-02-14 14:35
 * @vesion 1.0
 */
@ControllerAdvice(basePackages = "com.dhxz.search.web")
public class SearchResponseBodyAdvice implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(
            MethodParameter methodParameter,
            Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(
            Object obj,
            MethodParameter methodParameter,
            MediaType mediaType,
            Class<? extends HttpMessageConverter<?>> converterType,
            ServerHttpRequest serverHttpRequest,
            ServerHttpResponse serverHttpResponse) {

        if (obj == null) {
            ActionResult result = new ActionResult();
            result.setSuccess(true);
            result.setCode(200);
            return result;
        }
        if (obj instanceof ActionResult) {
            return obj;
        } else if (obj instanceof File) {
            return obj;
        } else if (obj instanceof String) {
            if (isJSONValid((String) obj)) {
                return obj;
            }
            return String.format("{\"success\":true,\"code\":200,\"data\":\"%s\"}", obj);
        } else {
            ActionResult result = new ActionResult();
            result.setSuccess(true);
            result.setCode(200);
            result.setData(obj);
            return result;
        }
    }

    private static boolean isJSONValid(String jsonInString) {
        try {
            final ObjectMapper mapper = new ObjectMapper();
            mapper.readTree(jsonInString);
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
