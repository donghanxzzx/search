package com.dhxz.search.domain;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.Data;

/**
 * @author 10066610
 * @description book 实体
 * @date 2019/3/6 17:27
 **/
@Entity
@Table(name = "tb_book")
@Data
public class Book {

    @Id
    @GeneratedValue
    private Long id;
    private String title;
    private String bookUrl;
    @ElementCollection
    private List<String> chapterList = new ArrayList<>();
}
