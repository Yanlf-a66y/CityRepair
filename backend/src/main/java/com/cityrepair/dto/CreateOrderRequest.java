package com.cityrepair.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class CreateOrderRequest {
    @NotNull(message = "报修类别不能为空")
    private Long categoryId;

    @NotBlank(message = "标题不能为空")
    private String title;

    @NotBlank(message = "位置不能为空")
    private String location;

    @NotBlank(message = "描述不能为空")
    private String description;

    @NotBlank(message = "联系电话不能为空")
    private String contactPhone;

    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getContactPhone() { return contactPhone; }
    public void setContactPhone(String contactPhone) { this.contactPhone = contactPhone; }
}
