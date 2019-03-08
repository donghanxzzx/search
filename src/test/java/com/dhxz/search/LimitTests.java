package com.dhxz.search;

import com.google.common.util.concurrent.RateLimiter;
import org.junit.Test;

import java.util.Collections;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class LimitTests {
    private final static RateLimiter limiter = RateLimiter.create(1.0);

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
}
