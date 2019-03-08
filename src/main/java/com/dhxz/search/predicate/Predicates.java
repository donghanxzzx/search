package com.dhxz.search.predicate;

import com.dhxz.search.domain.BookInfo;
import com.dhxz.search.domain.Chapter;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * @author 10066610
 * @description Stream Predicate
 * @date 2019/3/8 13:01
 **/
public class Predicates {

    public static Predicate<Chapter> hasNotCompletedChapter() {
        return item -> Objects.isNull(item.getCompleted()) || !item.getCompleted();
    }

    public static Predicate<BookInfo> hasNotCompletedBookInfo() {
        return item -> Objects.isNull(item.getCompleted()) || !item.getCompleted();
    }
}
