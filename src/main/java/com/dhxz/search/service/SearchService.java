package com.dhxz.search.service;

import com.dhxz.search.repository.BookRepository;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;

/**
 * @author 10066610
 * @description todo
 * @date 2019/3/6 19:17
 **/
@Slf4j
@Service
public class SearchService {

    private BookRepository bookRepository;
    private final String index = "http://www.55lewen.com";
    private final String full = "http://www.55lewen.com/full/";

    public void index() {
        Document document = get(index);
        System.out.println(document);
    }

    public void full() {
        Document document = get(full);
        System.out.println(document);
    }

    private Document get(String url) {
        Document document = null;
        try {
            document = Jsoup.connect(url).header(HttpHeaders.USER_AGENT,
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/72.0.3626.121 Safari/537.36")
                    .header(HttpHeaders.ACCEPT_LANGUAGE, "zh,zh-CN;q=0.9,en;q=0.8,zh-TW;q=0.7")
                    .get();
        } catch (Exception e) {
            log.error("请求错误:{}", e);
        }
        return document;
    }
}
