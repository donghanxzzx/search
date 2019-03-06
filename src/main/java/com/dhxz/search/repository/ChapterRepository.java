package com.dhxz.search.repository;

import com.dhxz.search.domain.BookInfo;
import com.dhxz.search.domain.Chapter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface ChapterRepository extends JpaRepository<Chapter,Long> {

    List<Chapter> findByBookInfoIdOrderByChapterOrder(Long bookInfoId);
}
