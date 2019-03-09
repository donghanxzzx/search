package com.dhxz.search.domain;

import lombok.Data;

import javax.persistence.*;

@Data
@Entity
@Table(name = "tb_page_line")
public class Line {

    @Id
    @GeneratedValue
    private Long id;

    private Integer lineOrder;

    @Lob
    private String content;
    @ManyToOne(cascade = {CascadeType.MERGE, CascadeType.REFRESH})
    @JoinColumn(name = "page_id", referencedColumnName = "id")
    private Page page;
}
