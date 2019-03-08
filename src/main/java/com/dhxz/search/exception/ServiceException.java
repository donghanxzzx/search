package com.dhxz.search.exception;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * @author 10066610
 * @description 服务异常
 * @date 2019/3/8 15:21
 **/
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(fluent = true, chain = true)
public class ServiceException extends RuntimeException {

    private ExceptionEnum exceptionEnum;

    public ServiceException(ExceptionEnum exceptionEnum) {
        super(exceptionEnum.getErrorMessage());
        this.exceptionEnum = exceptionEnum;
    }
}
