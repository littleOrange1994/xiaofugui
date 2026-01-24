package com.xiaofugui.service;

import cn.hutool.core.date.ChineseDate;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.xiaofugui.dto.AiRecommendResponse;
import com.xiaofugui.entity.Recipe;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
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
            你是美食推荐助手，从给定菜品中推荐一道。
            当前日期：%s %s

            要求：
            1. 从列表中选一道推荐
            2. 返回 JSON：{"recipeId": 数字, "reason": "40-60字推荐理由"}
            3. 只返回 JSON
            4. 理由要有趣有温度
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
            String festivalInfo = getFestivalInfo();
            String systemPrompt = String.format(SYSTEM_PROMPT,
                    LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy年MM月dd日")),
                    festivalInfo);
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

    private static final int CANDIDATE_COUNT = 10;

    /**
     * 构建菜品列表文本（随机选取指定数量的菜品）
     */
    private String buildRecipesText(List<Recipe> recipes) {
        // 打乱列表顺序，取前 N 个作为候选
        List<Recipe> shuffled = new ArrayList<>(recipes);
        Collections.shuffle(shuffled);
        List<Recipe> candidates = shuffled.subList(0, Math.min(CANDIDATE_COUNT, shuffled.size()));

        StringBuilder sb = new StringBuilder();
        for (Recipe recipe : candidates) {
            sb.append("[").append(recipe.getId()).append("] ");
            sb.append(recipe.getName());
            if (StrUtil.isNotBlank(recipe.getCategory())) {
                sb.append(" | ").append(recipe.getCategory());
            }
            if (recipe.getSpicyLevel() != null) {
                sb.append(" | 辣度:").append(recipe.getSpicyLevel());
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

            log.info("AI 推荐的菜品 ID: {}, 原因: {}", recipeId, reason);

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

    /**
     * 获取节日信息
     */
    private String getFestivalInfo() {
        LocalDate today = LocalDate.now();
        int month = today.getMonthValue();
        int day = today.getDayOfMonth();

        // 公历节日
        if (month == 1 && day == 1) {
            return "今天是元旦，推荐：饺子、年糕等喜庆美食";
        }
        if (month == 2 && day == 14) {
            return "今天是情人节，推荐：浪漫的甜点、巧克力相关美食";
        }
        if (month == 5 && day == 1) {
            return "今天是劳动节，推荐：丰盛的家常菜犒劳自己";
        }
        if (month == 6 && day == 1) {
            return "今天是儿童节，推荐：甜品、炸鸡等孩子喜欢的美食";
        }
        if (month == 10 && day == 1) {
            return "今天是国庆节，推荐：大菜、硬菜庆祝";
        }

        // 冬至（12月21-23日）
        if (month == 12 && day >= 21 && day <= 23) {
            return "今天是冬至，推荐：饺子、羊肉汤、汤圆等暖身美食";
        }

        // 清明节（4月4-6日）
        if (month == 4 && day >= 4 && day <= 6) {
            return "今天是清明节，推荐：青团、艾草糕等时令美食";
        }

        // 农历节日
        try {
            ChineseDate chineseDate = new ChineseDate(today);
            int lunarMonth = chineseDate.getMonth();
            int lunarDay = chineseDate.getDay();

            // 春节（正月初一至初七）
            if (lunarMonth == 1 && lunarDay >= 1 && lunarDay <= 7) {
                return "今天是春节期间，推荐：饺子、年糕、鱼（年年有余）、红烧肉等年夜饭美食";
            }
            // 元宵节（正月十五）
            if (lunarMonth == 1 && lunarDay == 15) {
                return "今天是元宵节，推荐：汤圆、元宵等团圆美食";
            }
            // 端午节（五月初五）
            if (lunarMonth == 5 && lunarDay == 5) {
                return "今天是端午节，推荐：粽子、咸鸭蛋等传统美食";
            }
            // 中秋节（八月十五）
            if (lunarMonth == 8 && lunarDay == 15) {
                return "今天是中秋节，推荐：月饼、螃蟹、桂花糕等团圆美食";
            }
            // 重阳节（九月初九）
            if (lunarMonth == 9 && lunarDay == 9) {
                return "今天是重阳节，推荐：重阳糕、菊花酒、羊肉等敬老美食";
            }
            // 腊八节（腊月初八）
            if (lunarMonth == 12 && lunarDay == 8) {
                return "今天是腊八节，推荐：腊八粥、腊八蒜等传统美食";
            }
            // 小年（腊月二十三）
            if (lunarMonth == 12 && lunarDay == 23) {
                return "今天是小年，推荐：糖瓜、饺子、灶糖等祭灶美食";
            }
            // 除夕（腊月三十或二十九）
            if (lunarMonth == 12 && (lunarDay == 29 || lunarDay == 30)) {
                return "今天是除夕，推荐：年夜饭大餐，饺子、鱼、红烧肉、扣肉等硬菜";
            }
        } catch (Exception e) {
            log.warn("农历日期转换失败", e);
        }

        return "";
    }

    private static final String AI_SEARCH_PROMPT = """
            你是专业厨师，用户想做"%s"。
            请提供详细的烹饪说明，包括：
            1. 所需食材和用量
            2. 详细步骤
            3. 小贴士
            
            用 Markdown 格式输出。
            """;

    /**
     * AI 搜索 - 根据关键词生成烹饪说明（SSE 流式返回）
     */
    public Flux<String> aiSearchStream(String keyword) {
        if (StrUtil.isBlank(keyword)) {
            return Flux.just("请输入要搜索的菜品名称");
        }

        try {
            String prompt = String.format(AI_SEARCH_PROMPT, keyword);
            ChatClient chatClient = chatClientBuilder.build();

            return chatClient.prompt()
                    .user(prompt)
                    .stream()
                    .content()
                    .doOnComplete(() -> log.info("AI 搜索流式响应完成, 关键词: {}", keyword))
                    .onErrorResume(e -> {
                        log.error("AI 搜索失败, 关键词: {}", keyword, e);
                        return Flux.just("AI 搜索失败，请稍后重试");
                    });
        } catch (Exception e) {
            log.error("AI 搜索初始化失败, 关键词: {}", keyword, e);
            return Flux.just("AI 搜索失败，请稍后重试");
        }
    }
}
