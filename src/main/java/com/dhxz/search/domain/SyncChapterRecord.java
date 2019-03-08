package com.dhxz.search.domain;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author 10066610
 * @description 同步章节信息记录
 * @date 2019/3/8 10:52
 **/
@Data
@Entity
@Table(name = "tb_sync_chapter_record")
@NoArgsConstructor
@AllArgsConstructor
public class SyncChapterRecord {

    @Id
    @GeneratedValue
    private Long id;

    private Integer beginBookInfoId;
    private Integer endBookInfoId;

    @ElementCollection
    private List<Integer> completedBookInfoId = new ArrayList<>();


}
