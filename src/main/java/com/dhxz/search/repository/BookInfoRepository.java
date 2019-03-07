package com.dhxz.search.repository;

import com.dhxz.search.domain.BookInfo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BookInfoRepository extends JpaRepository<BookInfo, Long> {

    List<BookInfo> findAllByBookOrderIn(List<Integer> orders);
}
