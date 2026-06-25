package com.cityrepair.dto;

/**
 * 状态分布统计
 */
public class StatusStat {

    private String status;
    private Long count;

    public StatusStat() {}

    public StatusStat(String status, Long count) {
        this.status = status;
        this.count = count;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getCount() {
        return count;
    }

    public void setCount(Long count) {
        this.count = count;
    }
}
