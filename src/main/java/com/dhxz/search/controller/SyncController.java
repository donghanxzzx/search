package com.dhxz.search.controller;

import com.dhxz.search.domain.BookInfo;
import com.dhxz.search.repository.BookInfoRepository;
import com.dhxz.search.service.SearchService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class SyncController {
    private SearchService searchService;
    private BookInfoRepository bookInfoRepository;
    private final String SUCCESS = "SUCCESS";

    public SyncController(SearchService searchService, BookInfoRepository bookInfoRepository) {
        this.searchService = searchService;
        this.bookInfoRepository = bookInfoRepository;
    }

    @GetMapping("/readBookInfo")
    public ResponseEntity<String> readBookInfo() {
        searchService.initAllVisitBookInfo();
        return ResponseEntity.ok(SUCCESS);
    }

    @GetMapping("/readChapter")
    public ResponseEntity<String> readChapter() {
        page().forEach(searchService::readChapter);
        return ResponseEntity.ok(SUCCESS);
    }

    @GetMapping("/readContent")
    public ResponseEntity<String> readContent() {
        page().forEach(searchService::readContent);
        return ResponseEntity.ok(SUCCESS);
    }

    private List<BookInfo> page() {
        return bookInfoRepository.findAll();
    }


}
