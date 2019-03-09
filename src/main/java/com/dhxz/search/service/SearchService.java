package com.dhxz.search.service;

import com.dhxz.search.domain.BookInfo;
import com.dhxz.search.domain.Chapter;
import com.dhxz.search.domain.Content;
import com.dhxz.search.predicate.Predicates;
import com.dhxz.search.repository.BookInfoRepository;
import com.dhxz.search.repository.ChapterRepository;
import com.dhxz.search.repository.ContentRepository;
import com.dhxz.search.vo.BookInfoVo;
import com.dhxz.search.vo.ThreadStatusVo;
import com.dhxz.search.web.utils.ClientUtil;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static com.dhxz.search.exception.ExceptionEnum.BOOK_NOT_FOUND;
import static com.dhxz.search.predicate.Predicates.hasNotCompletedChapter;
import static java.util.stream.Collectors.toList;

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
    private ThreadPoolTaskExecutor commonTaskExecutor;
    private ThreadPoolTaskExecutor contentTaskExecutor;
    private ClientUtil clientUtil;
    private final String next = "-->>";
    private final String base = "http://m.55lewen.com";
    private final String full = base + "/full/";
    private final String top = base + "/top.html";
    private final String topAllVisit = base + "/top-allvisit";
    private final Integer allVisitMaxPage = 741;


    public SearchService(ContentRepository contentRepository, ChapterRepository chapterRepository,
                         BookInfoRepository bookInfoRepository, ThreadPoolTaskExecutor commonTaskExecutor, ThreadPoolTaskExecutor contentTaskExecutor, ClientUtil clientUtil) {
        this.contentRepository = contentRepository;
        this.chapterRepository = chapterRepository;
        this.bookInfoRepository = bookInfoRepository;
        this.commonTaskExecutor = commonTaskExecutor;
        this.contentTaskExecutor = contentTaskExecutor;
        this.clientUtil = clientUtil;
    }

    public ThreadStatusVo checkThread() {
        int activeCount = commonTaskExecutor.getActiveCount();
        ThreadPoolExecutor executor =
                commonTaskExecutor.getThreadPoolExecutor();
        int size = executor.getQueue().size();
        int largestPoolSize = executor.getLargestPoolSize();
        long taskCount = executor.getTaskCount();
        long completedTaskCount = executor.getCompletedTaskCount();

        ThreadStatusVo vo = new ThreadStatusVo();
        vo.setActiveCount(activeCount);
        vo.setQueueSize(size);
        vo.setLargestPoolSize(largestPoolSize);
        vo.setTaskCount(taskCount);
        vo.setCompletedTaskCount(completedTaskCount);
        return vo;
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
        Document topAllVisit = clientUtil.get(vo.getInfoUrl());
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
                                chapterRepository.saveAndFlush(chapter);
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
                });

    }

    public void loadContext(Chapter chapter) {
        String uri = chapter.getUri();
        log.info("chapterUri:{}", uri);
        Document beginRead = clientUtil.get(base + uri);
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
            Document nextPage = clientUtil.get(base + nextPageUri);
            content(sb, nextPage);
            nextPageUri = getNextUri(nextPage);
        } while (!StringUtils.isEmpty(nextPageUri) && nextPageUri.startsWith(pattern));
        Content content = new Content();
        content.setContent(sb.toString());
        contentRepository.saveAndFlush(content);
        chapter.setContent(content);
        chapter.setCompleted(true);
        chapterRepository.saveAndFlush(chapter);
    }


    private List<Chapter> chapter(String url, List<Chapter> chapterList, BookInfo bookInfo) {
        final AtomicInteger count = new AtomicInteger(0);
        log.info("bookInfo:{}", bookInfo);
        Document document = clientUtil.get(url);
        if (Objects.nonNull(document)) {
            System.out.println(document);
            for (Element element : document.select(".ablum_read")) {
                if (element.text().contains("查看目录")) {
                    for (Element a : element.select("a")) {
                        if (a.text().contains("查看目录")) {
                            // 目录url
                            String currentUrl = base + a.attr("href");
                            do {
                                Document currentPage = clientUtil.get(currentUrl);
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
