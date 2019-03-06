package com.dhxz.search.repository;

import com.dhxz.search.domain.Chapter;
import com.dhxz.search.domain.Content;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface ContentRepository extends JpaRepository<Content, Long> {

}
