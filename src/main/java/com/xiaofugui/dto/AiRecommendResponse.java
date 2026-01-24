package com.xiaofugui.dto;

import com.xiaofugui.entity.Recipe;
import lombok.Data;

/**
 * AI 推荐响应
 */
@Data
public class AiRecommendResponse {

    /**
     * 推荐的菜品
     */
    private Recipe recipe;

    /**
     * AI 推荐理由
     */
    private String reason;

    /**
     * 季节提示
     */
    private String seasonTip;
}
