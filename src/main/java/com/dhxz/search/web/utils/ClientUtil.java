package com.dhxz.search.web.utils;

import com.google.common.util.concurrent.RateLimiter;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.http.HttpHeaders;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

/**
 * @author 10066610
 * @description Http客户端
 * @date 2019/3/8 20:12
 **/
@Slf4j
@Component
public class ClientUtil {

    private final static RateLimiter limiter = RateLimiter.create(15.0);

    public Document get(String url) {
        limiter.acquire();
        Document document = null;
        try {
            document = Jsoup.connect(url).timeout(3000).header(HttpHeaders.USER_AGENT,
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/72.0.3626.121 Safari/537.36")
                    .header(HttpHeaders.ACCEPT_LANGUAGE, "zh,zh-CN;q=0.9,en;q=0.8,zh-TW;q=0.7")
                    .get();
        } catch (Exception e) {
            log.error("请求错误:", e);
            throw new RuntimeException(e);
        }
        return document;
    }

    @Retryable(value = {Exception.class}, backoff = @Backoff(maxDelay = 500L))
    public Document postJson(String url, String jsonBody) {
        limiter.acquire();
        Document document = null;
        try {
            document = Jsoup.connect(url).header(HttpHeaders.USER_AGENT,
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/72.0.3626.121 Safari/537.36")
                    .header(HttpHeaders.ACCEPT_LANGUAGE, "zh,zh-CN;q=0.9,en;q=0.8,zh-TW;q=0.7")
                    .header(HttpHeaders.ACCEPT_ENCODING, "gzip, deflate, br")
                    .header(HttpHeaders.ACCEPT, "application/json")
                    .requestBody(jsonBody)
                    .post();
        } catch (Exception e) {
            log.error("请求错误:", e);
            throw new RuntimeException(e);
        }
        return document;
    }


}
