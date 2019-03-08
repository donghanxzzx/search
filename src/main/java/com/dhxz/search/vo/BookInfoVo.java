package com.dhxz.search.vo;

import com.dhxz.search.domain.BookInfo;
import lombok.Data;
import org.springframework.beans.BeanUtils;

@Data
public class BookInfoVo {

    private Long id;
    private String infoUrl;
    private Integer bookOrder;

    public static BookInfoVo toVo(BookInfo bookInfo) {
        BookInfoVo vo = new BookInfoVo();
        BeanUtils.copyProperties(bookInfo, vo);
        return vo;
    }
}
