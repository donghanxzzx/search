package com.dhxz.search;

import com.google.common.util.concurrent.RateLimiter;
import org.junit.Test;

import java.util.Collections;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class LimitTests {
    private final static RateLimiter limiter = RateLimiter.create(1.0);

    @Test
    public void test() {
        System.out.println(System.getProperty("java.io.tmpdir"));
    }

    @Test
    public void testLimit() {

        Collections.singleton(1)
                .parallelStream();
        Stream.iterate(1, i -> ++i)
                .limit(1_000_000)
                .collect(Collectors.toList())
                .stream()
                .forEach(item -> {
                    limiter.acquire();
                    System.out.println(item);
                });

    }

    @Test
    public void testS() {
        String target = "  &lt;b&gt;男生&lt;/b&gt; 第二章 撞破南墙不回头(第1/3页) ";
        String t2 = "cdgsddf";
        int start = target.indexOf("(");
        int end = target.indexOf(")");
        System.out.println(end);
        if (start != -1 && end != -1) {
            String s = target.substring(start + 1, end);
            System.out.println(s);
        }
    }
}
