package com.cityrepair.dto;

/**
 * 类别分布统计
 */
public class CategoryStat {

    private String categoryName;
    private Long count;

    public CategoryStat() {}

    public CategoryStat(String categoryName, Long count) {
        this.categoryName = categoryName;
        this.count = count;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public Long getCount() {
        return count;
    }

    public void setCount(Long count) {
        this.count = count;
    }
}
