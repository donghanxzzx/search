package com.dhxz.search.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Data
@Entity
@Table(name = "tb_content")
@NoArgsConstructor
@AllArgsConstructor
public class Content {
    @GeneratedValue
    @Id
    private Long id;

    @Lob
    private String content;

}
