package com.xiaofugui.dto;

import lombok.Data;

/**
 * 心愿单分配到周几请求
 */
@Data
public class WeeklyPlanAssignRequest {

    private Long recipeId;

    /**
     * 1=周一...7=周日，0/NULL 表示取消分配
     */
    private Integer planDay;
}

