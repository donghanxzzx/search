package com.dhxz.search.exception;

import java.util.function.Supplier;

/**
 * @author 10066610
 * @description 异常信息
 * @date 2019/3/8 15:22
 **/
public enum ExceptionEnum implements Supplier<ServiceException> {
    /**
     * 错误信息
     */
    BOOK_NOT_FOUND(10001, "未找到该书籍"),
    CHAPTER_NOT_COMPLETED(10002, "章节未同步完成,请过段时间重试"),
    CHAPTER_NOT_FOUND(10003,"未找到该章节"),
    ;
    private Integer errorCode;
    private String errorMessage;

    public Integer getErrorCode() {
        return errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    ExceptionEnum(Integer errorCode, String errorMessage) {
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }

    @Override
    public ServiceException get() {
        return new ServiceException(this);
    }
}
