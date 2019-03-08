package com.dhxz.search.vo;

import lombok.Data;

@Data
public class ThreadStatusVo {

    private int queueSize;
    private int activeCount;
    private int largestPoolSize;
    private long completedTaskCount;
    private long taskCount;
}
