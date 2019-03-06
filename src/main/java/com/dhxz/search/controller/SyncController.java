package com.dhxz.search.controller;

import com.dhxz.search.domain.BookInfo;
import com.dhxz.search.repository.BookInfoRepository;
import com.dhxz.search.service.SearchService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
public class SyncController {
    private SearchService searchService;
    private BookInfoRepository bookInfoRepository;

    public SyncController(SearchService searchService, BookInfoRepository bookInfoRepository) {
        this.searchService = searchService;
        this.bookInfoRepository = bookInfoRepository;
    }

    @GetMapping("/initAllVisitBookInfo")
    public void initBookInfo(){
        searchService.initAllVisitBookInfo();
    }

    @GetMapping("/readBook")
    public void readBook(@RequestParam(defaultValue = "1") Integer order){
        List<Integer> ordered = new ArrayList<>();
        for (int i = 1; i <= order; i++) {
            ordered.add(i);
        }
        List<BookInfo> infoList = bookInfoRepository.findAllByBookOrderIn(ordered);
        searchService.submitReadBook(infoList);
    }
}
