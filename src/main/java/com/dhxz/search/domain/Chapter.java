package com.dhxz.search.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Data
@Entity
@Table(name = "tb_chapter_info")
@NoArgsConstructor
@AllArgsConstructor
public class Chapter {
    @Id
    @GeneratedValue
    private Long id;
    private String chapterName;
    private String uri;

    private Long bookInfoId;

    private Long contentId;

    private Integer chapterOrder;
}
