package com.cityrepair.dto;

/**
 * 7天趋势数据项
 */
public class TrendItem {

    private String date;
    private Long count;

    public TrendItem() {}

    public TrendItem(String date, Long count) {
        this.date = date;
        this.count = count;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public Long getCount() {
        return count;
    }

    public void setCount(Long count) {
        this.count = count;
    }
}
