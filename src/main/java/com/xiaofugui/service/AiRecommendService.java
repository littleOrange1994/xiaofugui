package com.xiaofugui.service;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.xiaofugui.dto.AiRecommendResponse;
import com.xiaofugui.entity.Recipe;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Random;

/**
 * AI 推荐服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiRecommendService {

    private final ChatClient.Builder chatClientBuilder;
    private final RecipeService recipeService;

    private static final String SYSTEM_PROMPT = """
            你是一个专业的美食推荐助手。从给定菜品列表中推荐最合适的一道菜。
            当前日期：%s，当前季节：%s

            回复要求：
            1. 必须从给定列表中选择
            2. 以 JSON 格式返回：{"recipeId": 数字, "reason": "30-50字推荐理由"}
            3. 只返回 JSON，无其他文字
            4. 推荐理由要结合当前季节、天气特点，给出有温度的建议
            """;

    /**
     * AI 智能推荐菜品
     */
    public AiRecommendResponse recommend() {
        String season = getCurrentSeason();
        String seasonTip = getSeasonTip(season);

        try {
            List<Recipe> summaries = recipeService.getAllRecipeSummaries();
            if (summaries.isEmpty()) {
                return fallbackRecommend(seasonTip);
            }

            String recipesText = buildRecipesText(summaries);
            String systemPrompt = String.format(SYSTEM_PROMPT,
                    LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy年MM月dd日")),
                    season);
            String userMessage = "菜品列表：\n" + recipesText + "\n\n请推荐一道菜。";

            ChatClient chatClient = chatClientBuilder.build();
            String response = chatClient.prompt()
                    .system(systemPrompt)
                    .user(userMessage)
                    .call()
                    .content();

            return parseAiResponse(response, summaries, seasonTip);
        } catch (Exception e) {
            log.error("AI 推荐失败，降级为随机推荐", e);
            return fallbackRecommend(seasonTip);
        }
    }

    /**
     * 构建菜品列表文本
     */
    private String buildRecipesText(List<Recipe> recipes) {
        StringBuilder sb = new StringBuilder();
        for (Recipe recipe : recipes) {
            sb.append("[").append(recipe.getId()).append("] ");
            sb.append(recipe.getName());
            if (StrUtil.isNotBlank(recipe.getCategory())) {
                sb.append(" | ").append(recipe.getCategory());
            }
            if (recipe.getSpicyLevel() != null) {
                sb.append(" | 辣度:").append(recipe.getSpicyLevel());
            }
            if (StrUtil.isNotBlank(recipe.getSeasonTags())) {
                sb.append(" | 季节:").append(recipe.getSeasonTags());
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    /**
     * 解析 AI 响应
     */
    private AiRecommendResponse parseAiResponse(String response, List<Recipe> summaries, String seasonTip) {
        try {
            String jsonStr = extractJson(response);
            JSONObject json = JSONUtil.parseObj(jsonStr);
            Long recipeId = json.getLong("recipeId");
            String reason = json.getStr("reason");

            if (recipeId == null) {
                log.warn("AI 响应中没有 recipeId，降级为随机推荐");
                return fallbackRecommend(seasonTip);
            }

            Recipe recipe = recipeService.getRecipeById(recipeId);
            if (recipe == null) {
                log.warn("AI 推荐的菜品 ID {} 不存在，降级为随机推荐", recipeId);
                return fallbackRecommend(seasonTip);
            }

            AiRecommendResponse result = new AiRecommendResponse();
            result.setRecipe(recipe);
            result.setReason(reason);
            result.setSeasonTip(seasonTip);
            return result;
        } catch (Exception e) {
            log.error("解析 AI 响应失败: {}", response, e);
            return fallbackRecommend(seasonTip);
        }
    }

    /**
     * 从响应中提取 JSON
     */
    private String extractJson(String response) {
        if (StrUtil.isBlank(response)) {
            return "{}";
        }
        int start = response.indexOf("{");
        int end = response.lastIndexOf("}");
        if (start >= 0 && end > start) {
            return response.substring(start, end + 1);
        }
        return response;
    }

    /**
     * 降级为随机推荐
     */
    private AiRecommendResponse fallbackRecommend(String seasonTip) {
        List<Recipe> summaries = recipeService.getAllRecipeSummaries();
        if (summaries.isEmpty()) {
            AiRecommendResponse result = new AiRecommendResponse();
            result.setReason("暂无菜品推荐");
            result.setSeasonTip(seasonTip);
            return result;
        }

        Random random = new Random();
        Recipe randomRecipe = summaries.get(random.nextInt(summaries.size()));
        Recipe fullRecipe = recipeService.getRecipeById(randomRecipe.getId());

        AiRecommendResponse result = new AiRecommendResponse();
        result.setRecipe(fullRecipe);
        result.setReason("随机为您挑选的美味佳肴，希望您喜欢！");
        result.setSeasonTip(seasonTip);
        return result;
    }

    /**
     * 获取当前季节
     */
    private String getCurrentSeason() {
        int month = LocalDate.now().getMonthValue();
        if (month >= 3 && month <= 5) {
            return "春季";
        } else if (month >= 6 && month <= 8) {
            return "夏季";
        } else if (month >= 9 && month <= 11) {
            return "秋季";
        } else {
            return "冬季";
        }
    }

    /**
     * 获取季节提示
     */
    private String getSeasonTip(String season) {
        return switch (season) {
            case "春季" -> "春季养生 · 清淡为主";
            case "夏季" -> "夏季消暑 · 清凉解腻";
            case "秋季" -> "秋季进补 · 润燥滋养";
            case "冬季" -> "冬季暖身 · 滋补养生";
            default -> "美食推荐";
        };
    }
}
