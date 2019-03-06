package com.dhxz.search.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

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
}
