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

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 菜品名称
     */
    private String name;

    /**
     * 菜品分类（如：川菜、主食、炒菜、蒸菜等）
     */
    private String category;

    /**
     * 配料清单
     */
    private String ingredients;

    /**
     * 制作步骤
     */
    private String steps;

    /**
     * 菜品图片URL
     */
    private String imageUrl;

    /**
     * 推荐指数（1-10）
     */
    private Integer recommendScore;

    /**
     * 辣度等级（0-5，0表示不辣）
     */
    private Integer spicyLevel;

    /**
     * 养生功效
     */
    private String healthBenefit;

    /**
     * 市场参考价格
     */
    private BigDecimal marketPrice;

    /**
     * 季节标签（如：春季、夏季、四季皆宜）
     */
    private String seasonTags;

    /**
     * 节日标签（如：春节、中秋）
     */
    private String festivalTags;

    /**
     * 烹饪时间（分钟）
     */
    private Integer cookingTime;

    /**
     * 难度等级（1-5，1最简单）
     */
    private Integer difficulty;

    /**
     * 数据来源文件
     */
    private String sourceFile;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 是否为小曾推荐（0-否，1-是）
     */
    private Integer recommended;

    /**
     * 逻辑删除标记（0-未删除，1-已删除）
     */
    @TableLogic
    private Integer deleted;
}
