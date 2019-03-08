package com.dhxz.search.web.controller;

import com.dhxz.search.domain.BookInfo;
import com.dhxz.search.repository.BookInfoRepository;
import com.dhxz.search.service.OutputStreamService;
import com.dhxz.search.service.SearchService;
import com.dhxz.search.vo.BookInfoVo;
import com.dhxz.search.vo.ThreadStatusVo;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.util.List;

import static com.dhxz.search.exception.ExceptionEnum.BOOK_NOT_FOUND;
import static com.dhxz.search.predicate.Predicates.hasNotCompletedBookInfo;
import static java.util.stream.Collectors.toList;

@RestController
public class SyncController {
    private SearchService searchService;
    private OutputStreamService outputStreamService;
    private BookInfoRepository bookInfoRepository;
    private final String SUCCESS = "SUCCESS";

    public SyncController(SearchService searchService,
                          OutputStreamService outputStreamService,
                          BookInfoRepository bookInfoRepository) {
        this.searchService = searchService;
        this.outputStreamService = outputStreamService;
        this.bookInfoRepository = bookInfoRepository;
    }


    @GetMapping("/readBookInfo")
    public ResponseEntity<ThreadStatusVo> readBookInfo() {
        ThreadStatusVo vo = searchService.checkThread();
        if (vo.getActiveCount() == 0)
            searchService.initAllVisitBookInfo();
        return ResponseEntity.ok(vo);
    }

    @GetMapping("/readChapter")
    public ResponseEntity<ThreadStatusVo> readChapter() {
        ThreadStatusVo vo = searchService.checkThread();
        if (vo.getActiveCount() == 0)
            page().stream().map(BookInfoVo::toVo).forEach(searchService::readChapter);
        return ResponseEntity.ok(vo);
    }

    @GetMapping("/readContent")
    public ResponseEntity<ThreadStatusVo> readContent() {
        ThreadStatusVo vo = searchService.checkThread();
        if (vo.getActiveCount() == 0)
            pageAll().stream().map(BookInfoVo::toVo).forEach(searchService::readContent);
        return ResponseEntity.ok(vo);
    }

    private List<BookInfo> page() {
        return bookInfoRepository.findAll().stream()
                .filter(hasNotCompletedBookInfo())
                .collect(toList());
    }

    public void sync() {
        searchService.initAllVisitBookInfo();
        page().stream().map(BookInfoVo::toVo).forEach(searchService::readChapter);
        pageAll().stream().map(BookInfoVo::toVo).forEach(searchService::readContent);
    }

    @GetMapping("/download/{bookId}")
    public ResponseEntity<String> download(@PathVariable("bookId") Long bookId,
                                           HttpServletRequest request,
                                           HttpServletResponse response) {
        BookInfo book = bookInfoRepository.findById(bookId).orElseThrow(BOOK_NOT_FOUND);
        String path = System.getProperty("java.io.tmpdir");
        File file = new File(path + book.getTitle() + ".txt");
        if (file.exists()) {
            outputStreamService.readFromDisk(BookInfoVo.toVo(book), file, response);
        } else {
            outputStreamService.downloadBook(BookInfoVo.toVo(book), response);
        }
        return ResponseEntity.ok(SUCCESS);
    }

    private List<BookInfo> pageAll() {
        return bookInfoRepository.findAllByOrderByBookOrderAsc();
    }
}
