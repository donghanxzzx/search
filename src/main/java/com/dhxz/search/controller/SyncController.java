package com.dhxz.search.controller;

import com.dhxz.search.domain.BookInfo;
import com.dhxz.search.repository.BookInfoRepository;
import com.dhxz.search.service.SearchService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class SyncController {
    private SearchService searchService;
    private BookInfoRepository bookInfoRepository;

    public SyncController(SearchService searchService, BookInfoRepository bookInfoRepository) {
        this.searchService = searchService;
        this.bookInfoRepository = bookInfoRepository;
    }

    @GetMapping("/readBookInfo")
    public void readBookInfo() {
        searchService.initAllVisitBookInfo();
    }

    @GetMapping("/readChapter")
    public void readChapter() {
        page().forEach(searchService::readChapter);
    }

    @GetMapping("/readContent")
    public void readContent() {
        page().forEach(searchService::readContent);
    }

    private List<BookInfo> page() {
        return bookInfoRepository.findAll();
    }


}
