package com.dhxz.search.domain;

import lombok.Data;

import javax.persistence.*;

@Data
@Entity
@Table(name = "tb_chapter_page")
public class Page {

    @Id
    @GeneratedValue
    private Long id;
    private Integer pageSize;
    private Integer pageOrder;
    private String pageUri;
    @ManyToOne(cascade = {CascadeType.MERGE, CascadeType.REFRESH})
    @JoinColumn(name = "chapter_id", referencedColumnName = "id")
    private Chapter chapter;

    private Boolean completed;
}
