package com.dhxz.search.repository;

import com.dhxz.search.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PageRepository extends JpaRepository<Page, Long> {
    Optional<Page> findByPageUri(String pageUri);

    List<Page> findByChapterIdOrderByPageOrderAsc(Long chapterId);

    boolean existsByChapterIdAndCompletedFalse(Long chapterId);
}
