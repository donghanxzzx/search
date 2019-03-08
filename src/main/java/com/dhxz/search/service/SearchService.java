package com.dhxz.search.service;

import static com.dhxz.search.exception.ExceptionEnum.BOOK_NOT_FOUND;
import static com.dhxz.search.predicate.Predicates.hasNotCompletedChapter;
import static java.util.stream.Collectors.toList;

import com.dhxz.search.domain.BookInfo;
import com.dhxz.search.domain.Chapter;
import com.dhxz.search.domain.Content;
import com.dhxz.search.predicate.Predicates;
import com.dhxz.search.repository.BookInfoRepository;
import com.dhxz.search.repository.ChapterRepository;
import com.dhxz.search.repository.ContentRepository;
import com.dhxz.search.vo.BookInfoVo;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import javax.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.http.HttpHeaders;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
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

    private ExecutorService commonTaskExecutor = Executors.newFixedThreadPool(16,
            new ThreadFactory() {
                private AtomicInteger counter = new AtomicInteger(0);

                @Override
                public Thread newThread(Runnable r) {
                    Thread taskThread = new Thread(r);
                    taskThread.setName("CommonTaskPool-" + counter.addAndGet(1));
                    taskThread.setUncaughtExceptionHandler(
                            (t, e) -> log.error("当前任务执行失败 线程:{} 原因:{}", t, e));
                    return taskThread;
                }
            });
    private ExecutorService contentTaskExecutor = Executors.newFixedThreadPool(64,
            new ThreadFactory() {
                private AtomicInteger counter = new AtomicInteger(0);

                @Override
                public Thread newThread(Runnable r) {
                    Thread taskThread = new Thread(r);
                    taskThread.setName("ContentTaskPool-" + counter.addAndGet(1));
                    taskThread.setUncaughtExceptionHandler(
                            (t, e) -> log.error("当前任务执行失败 线程:{} 原因:{}", t, e));
                    return taskThread;
                }
            });

    public SearchService(ContentRepository contentRepository, ChapterRepository chapterRepository,
            BookInfoRepository bookInfoRepository) {
        this.contentRepository = contentRepository;
        this.chapterRepository = chapterRepository;
        this.bookInfoRepository = bookInfoRepository;
    }

    public void initAllVisitBookInfo() {
        List<BookInfoVo> infoVos = new ArrayList<>();
        for (int i = 1; i <= allVisitMaxPage; i++) {
            String url = topAllVisit + "-" + i + "/";
            BookInfoVo vo = new BookInfoVo();
            vo.setInfoUrl(url);
            vo.setBookOrder(i);
            infoVos.add(vo);
        }
        infoVos.forEach(item -> {
            commonTaskExecutor.execute(() -> {
                allVisitBookInfo(item);
            });
        });
    }

    @Transactional(rollbackOn = Exception.class)
    public void allVisitBookInfo(final BookInfoVo vo) {
        log.info("BookInfo:{}", vo);
        Document topAllVisit = get(vo.getInfoUrl());
        if (Objects.nonNull(topAllVisit)) {
            Elements bookInfoElements = topAllVisit.select(".cover").get(0).select(".blue");
            List<BookInfo> bookInfos = new ArrayList<>();
            for (Element bookInfoElement : bookInfoElements) {
                BookInfo bookInfo = new BookInfo();
                bookInfo.setTitle(bookInfoElement.text());
                bookInfo.setBookOrder(vo.getBookOrder());
                bookInfo.setInfoUrl(base + bookInfoElement.attr("href"));
                bookInfo.setCompleted(false);
                if (!bookInfoRepository.existsByInfoUrl(bookInfo.getInfoUrl())) {
                    bookInfos.add(bookInfo);
                }
            }
            bookInfoRepository.saveAll(bookInfos);
        }
    }

    public void readChapter(BookInfoVo vo) {
        commonTaskExecutor.execute(() -> {
            BookInfo infoInDb = bookInfoRepository.findById(vo.getId())
                    .orElseThrow(BOOK_NOT_FOUND);

            List<Chapter> chapter = chapter(infoInDb.getInfoUrl(), new ArrayList<>(), infoInDb);
            chapterRepository.saveAll(
                    chapter.stream().filter(Predicates.distinctByKey(Chapter::getUri))
                            .collect(toList()));
            infoInDb.setCompleted(true);
            bookInfoRepository.saveAndFlush(infoInDb);
        });
    }

    public void readContent(BookInfoVo vo) {
        commonTaskExecutor.execute(
                () -> {
                    log.info("bookInfo:{}", vo);
                    List<Chapter> chapters =
                            chapterRepository.findByBookInfoIdOrderByChapterOrderAsc(vo.getId())
                                    .stream()
                                    .filter(hasNotCompletedChapter())
                                    .collect(Collectors.toCollection(CopyOnWriteArrayList::new));
                    final CountDownLatch chapterLatch = new CountDownLatch(chapters.size());
                    for (Chapter chapter : chapters) {
                        contentTaskExecutor.execute(() -> {
                            try {
                                loadContext(chapter);
                            } catch (Exception e) {
                                chapter.setCompleted(false);
                                log.error("获取内容失败:{}", chapter);
                            } finally {
                                chapterLatch.countDown();
                            }
                        });
                    }
                    try {
                        chapterLatch.await();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    chapterRepository.saveAll(chapters);
        });
    }

    public void loadContext(Chapter chapter) {
        String uri = chapter.getUri();
        log.info("chapterUri:{}", uri);
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
        } while (!StringUtils.isEmpty(nextPageUri) && nextPageUri.startsWith(pattern));
        Content content = new Content();
        content.setContent(sb.toString());
        contentRepository.saveAndFlush(content);
        chapter.setContent(content);
        chapter.setCompleted(true);
    }


    private List<Chapter> chapter(String url, List<Chapter> chapterList, BookInfo bookInfo) {
        final AtomicInteger count = new AtomicInteger(0);
        log.info("bookInfo:{}", bookInfo);
        Document document = get(url);
        if (Objects.nonNull(document)) {
            System.out.println(document);
            for (Element element : document.select(".ablum_read")) {
                if (element.text().contains("查看目录")) {
                    for (Element a : element.select("a")) {
                        if (a.text().contains("查看目录")) {
                            // 目录url
                            String currentUrl = base + a.attr("href");
                            do {
                                Document currentPage = get(currentUrl);
                                // 如果请求到了数据
                                if (Objects.nonNull(currentPage)) {
                                    handleCurrentPage(currentPage, bookInfo, count, chapterList);
                                    currentUrl = next(currentPage.select(".page"), currentUrl);
                                }
                            } while (!StringUtils.isEmpty(currentUrl));
                        }
                    }
                }
            }
        }
        return chapterList;
    }

    private void handleCurrentPage(Document bookIndex, BookInfo bookInfo, AtomicInteger count, List<Chapter> chapterList) {
        Elements chapters = bookIndex.select(".chapter");
        if (!CollectionUtils.isEmpty(chapters)) {
            for (Element chapterEle : chapters) {
                Elements aList = chapterEle.select("a");
                if (!CollectionUtils.isEmpty(aList)) {
                    for (Element a : aList) {
                        String uri = a.attr("href");
                        log.info("chapterUri:{}", uri);
                        if (!chapterRepository.existsByUri(uri)) {
                            Chapter chapter = new Chapter();
                            chapter.setChapterName(a.text());
                            chapter.setUri(uri);
                            chapter.setBookInfo(bookInfo);
                            chapter.setChapterOrder(count.addAndGet(1));
                            chapter.setCompleted(false);
                            chapterList.add(chapter);
                        }
                    }
                }
            }
        }
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

    @Retryable(value = {Exception.class}, backoff = @Backoff(maxDelay = 500L))
    private Document get(String url) {
        Document document = null;
        try {
            document = Jsoup.connect(url).header(HttpHeaders.USER_AGENT,
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/72.0.3626.121 Safari/537.36")
                    .header(HttpHeaders.ACCEPT_LANGUAGE, "zh,zh-CN;q=0.9,en;q=0.8,zh-TW;q=0.7")
                    .get();
        } catch (Exception e) {
            log.error("请求错误:", e);
        }
        return document;
    }

    private String getNextUri(Document currentPage) {
        Elements elements = currentPage.select(".nr_page");
        if (!CollectionUtils.isEmpty(elements)) {
            return elements.select("#pb_next").get(0).attr("href");
        } else {
            return null;
        }
    }

    private String next(Elements page, String currentUrl) {
        if (!CollectionUtils.isEmpty(page) && page.get(0).text().contains("下一页")) {
            Elements aList = page.get(0).select("a");
            String nextUrl = "";
            for (Element element : aList) {
                if (element.text().contains("下一页")) {
                    nextUrl = base + element.attr("href");
                    if (StringUtils.pathEquals(currentUrl, nextUrl)) {
                        return null;
                    }
                }
            }
            if (!StringUtils.isEmpty(nextUrl)) {
                return nextUrl;
            } else {
                return null;
            }
        } else {
            return null;
        }
    }
}
