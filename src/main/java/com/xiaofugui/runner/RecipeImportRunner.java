package com.xiaofugui.runner;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.xiaofugui.entity.Recipe;
import com.xiaofugui.mapper.RecipeMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 菜谱数据导入
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RecipeImportRunner implements CommandLineRunner {

    private final RecipeMapper recipeMapper;

    @Value("${recipe.import.enabled:false}")
    private boolean importEnabled;

    @Value("${recipe.import.source-dir:/Users/pomazhangfei/Documents/CookLikeHOC}")
    private String sourceDir;

    private static final Pattern IMAGE_PATTERN = Pattern.compile("!\\[.*?]\\(\\.\\./(images/[^)]+)\\)");

    @Override
    public void run(String... args) {
        if (!importEnabled) {
            log.info("菜谱导入已禁用，跳过导入");
            return;
        }

        log.info("开始导入菜谱数据，源目录: {}", sourceDir);

        // 获取已存在的菜谱名称（用于去重）
        Set<String> existingNames = new HashSet<>();
        recipeMapper.selectList(null).forEach(r -> existingNames.add(r.getName()));
        log.info("数据库已有 {} 条菜谱", existingNames.size());

        // 分类目录映射
        Map<String, String> categoryMap = new HashMap<>();
        categoryMap.put("主食", "主食");
        categoryMap.put("凉拌", "凉拌");
        categoryMap.put("卤菜", "卤菜");
        categoryMap.put("早餐", "早餐");
        categoryMap.put("汤", "汤类");
        categoryMap.put("炒菜", "炒菜");
        categoryMap.put("炖菜", "炖菜");
        categoryMap.put("炸品", "炸品");
        categoryMap.put("烤类", "烤类");
        categoryMap.put("烫菜", "烫菜");
        categoryMap.put("煮锅", "煮锅");
        categoryMap.put("砂锅菜", "砂锅菜");
        categoryMap.put("蒸菜", "蒸菜");
        categoryMap.put("配料", "配料");
        categoryMap.put("饮品", "饮品");

        int importCount = 0;
        int updateCount = 0;
        int skipCount = 0;

        for (Map.Entry<String, String> entry : categoryMap.entrySet()) {
            String dirName = entry.getKey();
            String category = entry.getValue();
            File dir = new File(sourceDir, dirName);

            if (!dir.exists() || !dir.isDirectory()) {
                continue;
            }

            File[] files = dir.listFiles((d, name) -> name.endsWith(".md"));
            if (files == null) {
                continue;
            }

            for (File file : files) {
                try {
                    Recipe recipe = parseMarkdownFile(file, category);
                    if (recipe == null) {
                        continue;
                    }

                    // 检查是否已存在
                    QueryWrapper<Recipe> wrapper = new QueryWrapper<>();
                    wrapper.eq("name", recipe.getName());
                    Recipe existing = recipeMapper.selectOne(wrapper);

                    if (existing != null) {
                        // 更新已存在的记录（补充配料和步骤）
                        if (needsUpdate(existing, recipe)) {
                            existing.setIngredients(recipe.getIngredients());
                            existing.setSteps(recipe.getSteps());
                            if (StrUtil.isNotBlank(recipe.getImageUrl())) {
                                existing.setImageUrl(recipe.getImageUrl());
                            }
                            recipeMapper.updateById(existing);
                            updateCount++;
                            log.debug("更新菜谱: {}", recipe.getName());
                        } else {
                            skipCount++;
                        }
                    } else {
                        // 插入新记录
                        recipeMapper.insert(recipe);
                        importCount++;
                        log.debug("导入菜谱: {}", recipe.getName());
                    }
                } catch (Exception e) {
                    log.error("解析文件失败: {}", file.getAbsolutePath(), e);
                }
            }
        }

        log.info("菜谱导入完成: 新增 {} 条, 更新 {} 条, 跳过 {} 条", importCount, updateCount, skipCount);
    }

    /**
     * 检查是否需要更新
     */
    private boolean needsUpdate(Recipe existing, Recipe newRecipe) {
        // 如果现有记录的配料或步骤为空，需要更新
        boolean ingredientsEmpty = StrUtil.isBlank(existing.getIngredients()) || "[]".equals(existing.getIngredients());
        boolean stepsEmpty = StrUtil.isBlank(existing.getSteps()) || "[]".equals(existing.getSteps());
        boolean newIngredientsNotEmpty = StrUtil.isNotBlank(newRecipe.getIngredients()) && !"[]".equals(newRecipe.getIngredients());
        boolean newStepsNotEmpty = StrUtil.isNotBlank(newRecipe.getSteps()) && !"[]".equals(newRecipe.getSteps());

        return (ingredientsEmpty && newIngredientsNotEmpty) || (stepsEmpty && newStepsNotEmpty);
    }

    /**
     * 解析 Markdown 文件
     */
    private Recipe parseMarkdownFile(File file, String category) {
        String content = FileUtil.readString(file, StandardCharsets.UTF_8);
        if (StrUtil.isBlank(content)) {
            return null;
        }

        String[] lines = content.split("\n");
        if (lines.length == 0) {
            return null;
        }

        Recipe recipe = new Recipe();

        // 解析菜名（第一行 # 标题）
        String name = null;
        for (String line : lines) {
            line = line.trim();
            if (line.startsWith("# ") && !line.startsWith("## ")) {
                name = line.substring(2).trim();
                break;
            }
        }

        if (StrUtil.isBlank(name)) {
            // 使用文件名作为菜名
            name = file.getName().replace(".md", "");
        }
        recipe.setName(name);
        recipe.setCategory(category);
        recipe.setCuisineType("OTHER");
        recipe.setSourceFile(file.getAbsolutePath());

        // 解析图片 (格式: ../images/xxx.png -> /images/xxx.png)
        Matcher imageMatcher = IMAGE_PATTERN.matcher(content);
        if (imageMatcher.find()) {
            String imagePath = imageMatcher.group(1);
            // imagePath 是 "images/xxx.png"，转为 "/images/xxx.png"
            recipe.setImageUrl("/" + imagePath);
        }

        // 解析配料和步骤
        List<String> ingredients = new ArrayList<>();
        List<String> steps = new ArrayList<>();
        String currentSection = null;

        for (String line : lines) {
            line = line.trim();

            // 检测章节标题
            if (line.startsWith("## ")) {
                String sectionTitle = line.substring(3).trim();
                if (sectionTitle.contains("配料") || sectionTitle.contains("成分") || sectionTitle.contains("用料") || sectionTitle.contains("材料")) {
                    currentSection = "ingredients";
                } else if (sectionTitle.contains("步骤") || sectionTitle.contains("做法") || sectionTitle.contains("制作")) {
                    currentSection = "steps";
                } else {
                    currentSection = null;
                }
                continue;
            }

            // 解析列表项
            if (line.startsWith("- ") || line.startsWith("* ")) {
                String item = line.substring(2).trim();
                // 移除步骤编号前缀 (如 "1. ", "2. ")
                item = item.replaceFirst("^\\d+[.、．]\\s*", "");

                if ("ingredients".equals(currentSection) && StrUtil.isNotBlank(item)) {
                    ingredients.add(item);
                } else if ("steps".equals(currentSection) && StrUtil.isNotBlank(item)) {
                    steps.add(item);
                }
            }
        }

        // 转为 JSON 数组存储
        recipe.setIngredients(JSONUtil.toJsonStr(ingredients));
        recipe.setSteps(JSONUtil.toJsonStr(steps));

        // 设置默认值
        recipe.setRecommendScore(3);
        recipe.setSpicyLevel(0);
        recipe.setDifficulty(2);

        return recipe;
    }
}
