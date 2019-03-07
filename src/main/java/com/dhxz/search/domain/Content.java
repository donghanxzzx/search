package com.dhxz.search.domain;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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
    private Boolean completed;

}
