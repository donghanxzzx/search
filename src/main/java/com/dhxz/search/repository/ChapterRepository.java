package com.dhxz.search.repository;

import com.dhxz.search.domain.Chapter;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChapterRepository extends JpaRepository<Chapter, Long> {

    List<Chapter> findByBookInfoIdAndCompletedIsTrueOrderByChapterOrder(Long bookInfoId);

    boolean existsByUri(String uri);
}
