package com.dhxz.search.service;

import com.dhxz.search.domain.BookInfo;
import com.dhxz.search.domain.Chapter;
import com.dhxz.search.domain.Content;
import com.dhxz.search.repository.BookInfoRepository;
import com.dhxz.search.repository.ChapterRepository;
import com.dhxz.search.repository.ContentRepository;
import com.dhxz.search.vo.BookInfoVo;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import javax.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * @author 10066610
 * @description todo
 * @date 2019/3/6 19:17
 **/
@Slf4j
@Service
public class SearchService {

    private ContentRepository contentRepository;
    private ChapterRepository chapterRepository;
    private BookInfoRepository bookInfoRepository;
    private final String next = "-->>";
    private final String base = "http://m.55lewen.com";
    private final String full = base + "/full/";
    private final String top = base + "/top.html";
    private final String topAllVisit = base + "/top-allvisit";
    private final Integer allVisitMaxPage = 741;
    private final ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 2);

    public SearchService(ContentRepository contentRepository, ChapterRepository chapterRepository, BookInfoRepository bookInfoRepository) {
        this.contentRepository = contentRepository;
        this.chapterRepository = chapterRepository;
        this.bookInfoRepository = bookInfoRepository;
    }

    public Document index() {
        return get(base);
    }

    public Document top() {
        return get(top);
    }

    public Document topAllVisit() {
        return get(topAllVisit);
    }

    public Document full() {
        return get(full);
    }

    public void initAllVisitBookInfo() {
        final CountDownLatch latch = new CountDownLatch(allVisitMaxPage);
        List<BookInfoVo> infoVos = new ArrayList<>();
        for (int i = 1; i <= allVisitMaxPage; i++) {
            String url = topAllVisit + "-" + i + "/";
            BookInfoVo vo = new BookInfoVo();
            vo.setInfoUrl(url);
            vo.setBookOrder(i);
            infoVos.add(vo);
        }
        infoVos.forEach(item -> {
            executorService.execute(() -> {
                try {
                    allVisitBookInfo(item);
                } finally {
                    latch.countDown();
                    log.info("还剩余:{}", latch.getCount());
                }
            });
        });

        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        log.info("初始化bookInfo完成");


    }

    @Transactional(rollbackOn = Exception.class)
    public void allVisitBookInfo(final BookInfoVo vo) {

        Document topAllVisit = get(vo.getInfoUrl());

        Elements bookInfoElements = topAllVisit.select(".cover").get(0).select(".blue");
        List<BookInfo> bookInfos = new ArrayList<>();
        for (Element bookInfoElement : bookInfoElements) {
            BookInfo bookInfo = new BookInfo();
            bookInfo.setTitle(bookInfoElement.text());
            bookInfo.setBookOrder(vo.getBookOrder());
            bookInfo.setInfoUrl(base + bookInfoElement.attr("href"));
            bookInfo.setCompleted(true);
            bookInfos.add(bookInfo);
        }

        bookInfoRepository.saveAll(bookInfos);
    }

    @Transactional(rollbackOn = Exception.class)
    public void readChapter(BookInfo bookInfo) {
        chapterRepository.saveAll(chapter(bookInfo.getInfoUrl(), new ArrayList<>(), bookInfo));
    }

    @Transactional(rollbackOn = Exception.class)
    public void readContent(BookInfo bookInfo) {
        List<Chapter> chapters = chapterRepository
                .findByBookInfoIdAndCompletedIsFalseOrderByChapterOrder(bookInfo.getId());

        final CountDownLatch latch = new CountDownLatch(chapters.size());
        for (Chapter chapter : chapters) {
            executorService.execute(() -> {
                try {
                    loadContext(chapter);
                } finally {
                    latch.countDown();
                }
            });
        }
        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        chapterRepository.saveAll(chapters);
    }

    public void loadContext(Chapter chapter) {
        String uri = chapter.getUri();
        log.info("chapterUri:{}",uri);
        Document beginRead = get(base + uri);
        StringBuilder sb = new StringBuilder();
        content(sb, beginRead);
        String pattern;
        if (uri.endsWith("/")) {
            pattern = uri.substring(0, uri.length() - 1);
        } else {
            pattern = uri;
        }
        String nextPageUri = getNextUri(beginRead);

        do {
            Document nextPage = get(base + nextPageUri);
            content(sb, nextPage);
            nextPageUri = getNextUri(nextPage);
        } while (nextPageUri.startsWith(pattern));
        Content content = new Content();
        content.setContent(sb.toString());
        content.setCompleted(true);
        contentRepository.saveAndFlush(content);
        chapter.setContentId(content.getId());
        chapter.setCompleted(true);
    }



    private List<Chapter> chapter(String url, List<Chapter> chapterList, BookInfo bookInfo) {
        final AtomicInteger count = new AtomicInteger(0);
        Document document = get(url);
        Elements chapters = document.select(".chapter");
        if (!CollectionUtils.isEmpty(chapters)) {
            for (Element chapterEle : chapters) {
                Elements aList = chapterEle.select("a");
                if (!CollectionUtils.isEmpty(aList)) {
                    for (Element a : aList) {
                        Chapter chapter = new Chapter();
                        chapter.setChapterName(a.text());
                        String uri = a.attr("href");
                        chapter.setUri(uri);
                        chapter.setBookInfoId(bookInfo.getId());
                        chapter.setChapterOrder(count.addAndGet(1));
                        chapterList.add(chapter);
                    }
                }
            }
        }

        String nextUrl = next(document.select(".page"));
        if (!StringUtils.isEmpty(nextUrl)) {
            chapter(nextUrl, chapterList, bookInfo);
        }
        return chapterList;
    }

    private void content(StringBuilder context, Document page) {
        Elements select = page.select("#nr1");
        for (Element doc : select) {
            String text = doc.text();
            if (text.contains(next)) {
                context.append(text, 0, text.indexOf(next));
            } else {
                context.append(text);
            }
        }
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

    private String getNextUri(Document currentPage) {
        return currentPage.select(".nr_page")
                .select("#pb_next").get(0).attr("href");
    }

    private String next(Elements page) {
        if (!CollectionUtils.isEmpty(page) && page.get(0).text().contains("下一页")) {
            Elements aList = page.get(0).select("a");
            String nextUrl = "";
            for (Element element : aList) {
                if (element.text().contains("下一页")) {
                    nextUrl = base + element.attr("href");
                }
            }
            if (!StringUtils.isEmpty(nextUrl)) {
                return nextUrl;
            } else {
                return null;
            }
        } else if (!CollectionUtils.isEmpty(page) && page.get(0).text().contains("下一章")) {
            Elements aList = page.get(0).select("a");
            String nextUrl = "";
            for (Element element : aList) {
                if (element.text().contains("下一章")) {
                    nextUrl = base + element.attr("href");
                }
            }
            System.out.println(nextUrl);
            if (!StringUtils.isEmpty(nextUrl)) {
                return nextUrl;
            } else {
                return null;
            }
        }
        return null;
    }
}
