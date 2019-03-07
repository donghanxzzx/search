package com.dhxz.search.repository;

import com.dhxz.search.domain.Chapter;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChapterRepository extends JpaRepository<Chapter,Long> {

    List<Chapter> findByBookInfoIdAndCompletedIsFalseOrderByChapterOrder(Long bookInfoId);
}
