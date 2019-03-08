package com.dhxz.search.web;

import lombok.Data;

/**
 * @ClassName ActionResult
 * @Author berniewu
 * @Date 2019-02-14 14:11
 * @Vesion 1.0
 **/
@Data
public class ActionResult {

    private boolean success;

    private Object data;

    private Integer code;

}
