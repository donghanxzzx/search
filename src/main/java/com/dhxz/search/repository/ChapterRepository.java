package com.dhxz.search.repository;

import com.dhxz.search.domain.BookInfo;
import com.dhxz.search.domain.Chapter;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChapterRepository extends JpaRepository<Chapter, Long> {

    List<Chapter> findByBookInfoIdOrderByChapterOrderAsc(Long bookInfoId);

    boolean existsByBookInfoAndCompleted(BookInfo bookInfo, Boolean isCompleted);

    boolean existsByUri(String uri);

}
