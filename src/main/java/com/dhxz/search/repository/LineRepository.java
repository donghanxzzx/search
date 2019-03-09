package com.dhxz.search.repository;

import com.dhxz.search.domain.Line;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LineRepository extends JpaRepository<Line, Long> {

    List<Line> findByPageIdOrderByLineOrderAsc(Long pageId);
}
