package com.dhxz.search.repository;

import com.dhxz.search.domain.BookInfo;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookInfoRepository extends JpaRepository<BookInfo, Long> {

    List<BookInfo> findAllByBookOrderIn(List<Integer> orders);

    Boolean existsByInfoUrl(String infoUrl);
}
