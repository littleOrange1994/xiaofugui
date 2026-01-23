package com.xiaofugui.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 菜谱实体
 */
@Data
@TableName("recipe")
public class Recipe {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String cuisineType;

    private String category;

    private String ingredients;

    private String steps;

    private String imageUrl;

    private Integer recommendScore;

    private Integer spicyLevel;

    private String healthBenefit;

    private BigDecimal marketPrice;

    private String seasonTags;

    private String festivalTags;

    private Integer cookingTime;

    private Integer difficulty;

    private String sourceFile;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
