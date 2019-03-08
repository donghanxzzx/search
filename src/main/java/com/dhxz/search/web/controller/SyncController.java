package com.dhxz.search.web.controller;

import static com.dhxz.search.exception.ExceptionEnum.BOOK_NOT_FOUND;
import static com.dhxz.search.predicate.Predicates.hasNotCompletedBookInfo;
import static java.util.stream.Collectors.toList;

import com.dhxz.search.domain.BookInfo;
import com.dhxz.search.repository.BookInfoRepository;
import com.dhxz.search.service.OutputStreamService;
import com.dhxz.search.service.SearchService;
import com.dhxz.search.vo.BookInfoVo;
import java.util.List;
import javax.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

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
    public ResponseEntity<String> readBookInfo() {
        searchService.initAllVisitBookInfo();
        return ResponseEntity.ok(SUCCESS);
    }

    @GetMapping("/readChapter")
    public ResponseEntity<String> readChapter() {
        page().stream().map(BookInfoVo::toVo).forEach(searchService::readChapter);
        return ResponseEntity.ok(SUCCESS);
    }

    @GetMapping("/readContent")
    public ResponseEntity<String> readContent() {
        pageAll().stream().map(BookInfoVo::toVo).forEach(searchService::readContent);
        return ResponseEntity.ok(SUCCESS);
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
            HttpServletResponse response) {
        BookInfo book = bookInfoRepository.findById(bookId).orElseThrow(BOOK_NOT_FOUND);

        outputStreamService.downloadBook(BookInfoVo.toVo(book), response);
        return ResponseEntity.ok(SUCCESS);
    }

    private List<BookInfo> pageAll() {
        return bookInfoRepository.findAll();
    }
}
