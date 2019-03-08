package com.dhxz.search;

import com.google.common.util.concurrent.RateLimiter;
import org.junit.Test;

import java.util.stream.Stream;

public class LimitTests {
    private final static RateLimiter limiter = RateLimiter.create(15.0);

    @Test
    public void testLimit() {

        Stream.iterate(1, i -> ++i)
                .forEach(item -> {
                    limiter.acquire();
                    System.out.println(item);
                });

    }
}
