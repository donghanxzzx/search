package com.dhxz.search;

import com.dhxz.search.domain.BookInfo;
import com.dhxz.search.domain.Chapter;
import com.dhxz.search.domain.Content;
import com.dhxz.search.repository.BookInfoRepository;
import com.dhxz.search.repository.ChapterRepository;
import com.dhxz.search.repository.ContentRepository;
import com.dhxz.search.service.SearchService;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
public class SearchApplicationTests {
    private final String base = "http://m.55lewen.com";

    @Autowired
    private ContentRepository contentRepository;
    @Autowired
    private SearchService searchService;
    @Autowired
    private BookInfoRepository bookInfoRepository;
    @Autowired
    private ChapterRepository chapterRepository;
    private final ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    @Test
    public void contextLoads() throws InterruptedException {

    }

    @Test
    public void all() {
        searchService.initAllVisitBookInfo();
    }

    @Test
    public void readBook() {
        List<Integer> orders = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            orders.add(i);
        }
        List<BookInfo> infoList = bookInfoRepository
                .findAllByBookOrderIn(Collections.singletonList(1));
        System.out.println(infoList.size());

    }


    @Test
    public void testOutputFile() throws IOException {
        List<BookInfo> all = bookInfoRepository.findAll();
        System.out.println(all.size());
        BookInfo bookInfo = all.get(0);
        List<Chapter> chapters = chapterRepository
                .findByBookInfoIdAndOrderByChapterOrder(bookInfo.getId());
        FileOutputStream fos = new FileOutputStream(new File(bookInfo.getTitle() + ".txt"));
        chapters.forEach(item -> {
            Optional<Content> optional = contentRepository.findById(item.getContentId());
            if (optional.isPresent()) {
                try {
                    String content = optional.get().getContent();
                    content = "\t" + content + "\r\n";
                    fos.write(content.getBytes());
                    fos.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

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
