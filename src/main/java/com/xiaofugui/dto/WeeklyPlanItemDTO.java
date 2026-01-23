package com.xiaofugui.dto;

import com.xiaofugui.entity.Recipe;
import lombok.Data;

/**
 * 心愿单条目
 */
@Data
public class WeeklyPlanItemDTO {

    private Recipe recipe;

    /**
     * 1=周一...7=周日，NULL=未分配
     */
    private Integer planDay;

    /**
     * 备注（最多 200 字）
     */
    private String remark;
}

