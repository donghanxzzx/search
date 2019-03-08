package com.dhxz.search.vo;

import lombok.Value;

/**
 * @author 10066610
 * @description 数据转换
 * @date 2019/3/8 11:18
 **/
@Value
public class ChapterVo {

    private Long id;
    private String chapterName;
    private String uri;
    private Long bookInfoId;
    private Long contentId;
    private Integer bookOrder;
    private Integer chapterOrder;
    private Boolean completed;
}
