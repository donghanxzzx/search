package com.dhxz.search.repository;

import com.dhxz.search.domain.BookInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

public interface BookInfoRepository extends JpaRepository<BookInfo,Long> {

    List<BookInfo> findAllByBookOrderIn(List<Integer> orders);
}
