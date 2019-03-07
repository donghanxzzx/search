package com.dhxz.search.domain;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@Table(name = "tb_book_info")
@NoArgsConstructor
@AllArgsConstructor
public class BookInfo {

    @Id
    @GeneratedValue
    private Long id;
    private String title;
    private String infoUrl;
    private Integer bookOrder;
    private Boolean completed;
}
