package com.xiaofugui.dto;

import lombok.Data;

/**
 * 心愿单备注请求
 */
@Data
public class WeeklyPlanRemarkRequest {

    private Long recipeId;

    /**
     * 备注（最多 200 字），空字符串表示清空
     */
    private String remark;
}

