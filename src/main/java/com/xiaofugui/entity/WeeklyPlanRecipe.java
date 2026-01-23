package com.xiaofugui.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 本周计划-菜谱关联实体
 */
@Data
@TableName("weekly_plan_recipe")
public class WeeklyPlanRecipe {

    @TableId(type = IdType.AUTO)
    private Long id;

    private LocalDateTime weekStartTime;

    private Long recipeId;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}

