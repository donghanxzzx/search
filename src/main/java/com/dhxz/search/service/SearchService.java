package com.dhxz.search.service;

import com.dhxz.search.config.SyncProperties;
import com.dhxz.search.domain.BookInfo;
import com.dhxz.search.domain.Chapter;
import com.dhxz.search.domain.Line;
import com.dhxz.search.domain.Page;
import com.dhxz.search.predicate.Predicates;
import com.dhxz.search.repository.BookInfoRepository;
import com.dhxz.search.repository.ChapterRepository;
import com.dhxz.search.repository.LineRepository;
import com.dhxz.search.repository.PageRepository;
import com.dhxz.search.vo.BookInfoVo;
import com.dhxz.search.vo.ThreadStatusVo;
import com.dhxz.search.web.utils.ClientUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static com.dhxz.search.exception.ExceptionEnum.BOOK_NOT_FOUND;
import static com.dhxz.search.exception.ExceptionEnum.CHAPTER_NOT_FOUND;
import static com.dhxz.search.predicate.Predicates.hasNotCompletedChapter;
import static com.google.common.collect.Lists.newArrayList;
import static java.util.stream.Collectors.toList;

/**
 * @author 10066610
 * @description todo
 * @date 2019/3/6 19:17
 **/
@Slf4j
@Service
public class SearchService {

    private SyncProperties syncProperties;
    private ChapterRepository chapterRepository;
    private BookInfoRepository bookInfoRepository;
    private PageRepository pageRepository;
    private LineRepository lineRepository;
    private ThreadPoolTaskExecutor commonTaskExecutor;
    private ThreadPoolTaskExecutor contentTaskExecutor;
    private ClientUtil clientUtil;
    private final String base = "http://m.55lewen.com";
    private final String full = base + "/full/";
    private final String top = base + "/top.html";


    public SearchService(SyncProperties syncProperties, ChapterRepository chapterRepository,
                         BookInfoRepository bookInfoRepository,
                         PageRepository pageRepository,
                         LineRepository lineRepository,
                         ThreadPoolTaskExecutor commonTaskExecutor,
                         ThreadPoolTaskExecutor contentTaskExecutor,
                         ClientUtil clientUtil) {
        this.syncProperties = syncProperties;
        this.chapterRepository = chapterRepository;
        this.bookInfoRepository = bookInfoRepository;
        this.pageRepository = pageRepository;
        this.lineRepository = lineRepository;
        this.commonTaskExecutor = commonTaskExecutor;
        this.contentTaskExecutor = contentTaskExecutor;
        this.clientUtil = clientUtil;
    }

    public ThreadStatusVo checkCommendThread() {
        return getThreadStatus(commonTaskExecutor);
    }

    public ThreadStatusVo checkContentThread() {
        return getThreadStatus(contentTaskExecutor);
    }

    private ThreadStatusVo getThreadStatus(ThreadPoolTaskExecutor contentTaskExecutor) {
        int activeCount = contentTaskExecutor.getActiveCount();
        ThreadPoolExecutor executor =
                contentTaskExecutor.getThreadPoolExecutor();
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
        for (int i = 1; i <= syncProperties.getSyncPage(); i++) {
            String topUrl = base + "/top-allvisit";
            String url = topUrl + "-" + i + "/";
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

        log.info("bookInfo:{}", vo);
        List<Chapter> chapters =
                chapterRepository.findByBookInfoIdOrderByChapterOrderAsc(vo.getId())
                        .stream()
                        .filter(hasNotCompletedChapter())
                        .collect(Collectors.toCollection(CopyOnWriteArrayList::new));

        for (Chapter chapter : chapters) {
            try {
                loadContextWithPageLine(chapter);
            } catch (Exception e) {
                chapter.setCompleted(false);
                chapterRepository.saveAndFlush(chapter);
                log.error("获取内容失败:{}", chapter);
            }
        }
    }

    /**
     * 带行的内容
     *
     * @param chapter 章节
     */
    public void loadContextWithPageLine(Chapter chapter) {

        contentTaskExecutor.execute(() -> {
            String uri = chapter.getUri();
            Chapter chapterInDb = chapterRepository.findById(chapter.getId()).orElseThrow(CHAPTER_NOT_FOUND);
            log.info("chapterUri:{}", uri);
            Document currentPage = clientUtil.get(base + uri);
            int pageSize = handlePageSize(currentPage);
            String uriPrefix = uri.substring(0, uri.lastIndexOf("/"));
            for (int i = 0; i < pageSize; i++) {
                Integer index = i + 1;
                String pageUri = uriPrefix + "-" + index + "/";
                handleContentPage(index, pageUri, chapterInDb, pageSize);

            }
            // 判断该章节是否处理完成
            if (!pageRepository.existsByChapterIdAndCompletedFalse(chapterInDb.getId())) {
                chapterInDb.setCompleted(true);
                chapterRepository.saveAndFlush(chapterInDb);
            }
        });
    }

    private void handleContentPage(Integer pageOrder, String uri, Chapter chapterInDb, int pageSize) {
        log.info("pageUri:{}", uri);
        Document currentPage = clientUtil.get(base + uri);
        if (Objects.nonNull(currentPage)) {

            Optional<Page> pageUriOptional = pageRepository.findByPageUri(uri);
            Elements nrNrs = currentPage.select(".nr_nr");
            // 内容
            Elements contentEle = nrNrs.select("#nr1");
            if (!pageUriOptional.isPresent()) {

                Page page = new Page();
                page.setChapter(chapterInDb);
                page.setPageOrder(pageOrder);
                page.setPageUri(uri);
                page.setPageSize(pageSize);
                page.setCompleted(false);
                pageRepository.saveAndFlush(page);
                handleLine(contentEle, page);
                pageRepository.saveAndFlush(page);
            } else if (!pageUriOptional.get().getCompleted()) {
                Page page = pageUriOptional.get();
                List<Line> lines = lineRepository.findByPageIdOrderByLineOrderAsc(page.getId());
                lineRepository.deleteAll(lines);
                handleLine(contentEle, page);
                pageRepository.saveAndFlush(page);
            }
        }
    }


    private int handlePageSize(Document firstPage) {
        Elements titleEls = firstPage.select("#nr_title");
        log.info("titleElements:{}", titleEls);
        if (!CollectionUtils.isEmpty(titleEls)) {
            String title = titleEls.get(0).text();
            int start = title.indexOf("(");
            int end = title.indexOf(")");

            title = title.substring(start + 1, end);
            if (title.contains("页")) {
                String[] split = title.split("/");
                if (split.length > 1) {
                    String s = split[1];
                    int idP = s.indexOf("页");
                    if (idP != -1) {
                        String pageSizeStr = s.substring(0, idP);
                        return Integer.valueOf(pageSizeStr);
                    }
                }
            }

        }
        return 0;
    }

    private void handleLine(Elements contentEle, Page page) {
        List<Line> lineList = newArrayList();
        for (Element element : contentEle) {
            List<Node> nodes = element.childNodes();
            int i = 0;
            for (Node node : nodes) {
                String s = node.toString();
                s = s.replaceAll("<br>", "");
                if (StringUtils.isNotBlank(s)) {
                    s = cleanContent(s);
                    if (StringUtils.isNotBlank(s)) {
                        i = ++i;
                        Line line = new Line();
                        line.setLineOrder(i);
                        line.setPage(page);
                        line.setContent(s.trim());
                        lineList.add(line);
                    }
                }
            }
        }
        page.setCompleted(true);
        lineRepository.saveAll(lineList);
    }

    private String cleanContent(String content) {
        content = content.replaceAll("&nbsp;", "")
                .replaceAll("nbsp;", "")
                .replaceAll("bsp;", "")
                .replaceAll("sp;", "")
                .replaceAll("p;", "")
                .replaceAll("--&gt;&gt;", "")
                .replaceAll("&amn", "")
                .replaceAll("&am", "")
                .replaceAll("&a", "")
                .replaceAll("&", "")
                .replaceAll("b", "")
                .replaceAll("-->>", "")
                .replaceAll("本章未完，点击下一页继续阅读", "")
                .trim();
        return content;
    }

    private List<Chapter> chapter(String url, List<Chapter> chapterList, BookInfo bookInfo) {
        final AtomicInteger count = new AtomicInteger(0);
        log.info("bookInfo:{}", bookInfo);
        Document document = clientUtil.get(url);
        if (Objects.nonNull(document)) {
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
            String next = "-->>";
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
                    if (StringUtils.equals(currentUrl, nextUrl)) {
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
