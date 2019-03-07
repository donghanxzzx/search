package com.dhxz.search.controller;

import com.dhxz.search.domain.BookInfo;
import com.dhxz.search.repository.BookInfoRepository;
import com.dhxz.search.service.SearchService;
import java.util.ArrayList;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
    public void readChapter(@RequestParam(defaultValue = "1") Integer order) {
        page(order).forEach(searchService::readChapter);
    }

    @GetMapping("/readContent")
    public void readContent(@RequestParam(defaultValue = "1") Integer order) {
        page(order).forEach(searchService::readContent);
    }

    private List<BookInfo> page(
            @RequestParam(defaultValue = "1") Integer order) {
        List<Integer> ordered = new ArrayList<>();
        for (int i = 1; i <= order; i++) {
            ordered.add(i);
        }
        return bookInfoRepository.findAllByBookOrderInAndCompletedFalse(ordered);
    }


}
