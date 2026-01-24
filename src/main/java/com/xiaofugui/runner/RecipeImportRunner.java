package com.xiaofugui.runner;

import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.xiaofugui.entity.Recipe;
import com.xiaofugui.mapper.RecipeMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 菜谱数据导入（支持从 classpath 读取）
 */
@Slf4j
//@Component
@RequiredArgsConstructor
public class RecipeImportRunner implements CommandLineRunner {

    private final RecipeMapper recipeMapper;

    @Value("${recipe.import.enabled:false}")
    private boolean importEnabled;

    private static final Pattern IMAGE_PATTERN = Pattern.compile("!\\[.*?]\\(\\.\\./(images/[^)]+)\\)");

    private static final Map<String, String> CATEGORY_MAP = new HashMap<>();

    static {
        CATEGORY_MAP.put("主食", "主食");
        CATEGORY_MAP.put("凉拌", "凉拌");
        CATEGORY_MAP.put("卤菜", "卤菜");
        CATEGORY_MAP.put("早餐", "早餐");
        CATEGORY_MAP.put("汤", "汤类");
        CATEGORY_MAP.put("炒菜", "炒菜");
        CATEGORY_MAP.put("炖菜", "炖菜");
        CATEGORY_MAP.put("炸品", "炸品");
        CATEGORY_MAP.put("烤类", "烤类");
        CATEGORY_MAP.put("烫菜", "烫菜");
        CATEGORY_MAP.put("煮锅", "煮锅");
        CATEGORY_MAP.put("砂锅菜", "砂锅菜");
        CATEGORY_MAP.put("蒸菜", "蒸菜");
        CATEGORY_MAP.put("配料", "配料");
        CATEGORY_MAP.put("饮品", "饮品");
    }

    @Override
    public void run(String... args) {
        if (!importEnabled) {
            log.info("菜谱导入已禁用，跳过导入");
            return;
        }

        log.info("开始从 classpath 导入菜谱数据");

        int importCount = 0;
        int updateCount = 0;
        int skipCount = 0;

        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

        for (Map.Entry<String, String> entry : CATEGORY_MAP.entrySet()) {
            String dirName = entry.getKey();
            String category = entry.getValue();

            try {
                String pattern = "classpath:recipes/" + dirName + "/*.md";
                Resource[] resources = resolver.getResources(pattern);

                for (Resource resource : resources) {
                    try {
                        Recipe recipe = parseMarkdownResource(resource, category);
                        if (recipe == null) {
                            continue;
                        }

                        // 检查是否已存在（按名称去重）
                        QueryWrapper<Recipe> wrapper = new QueryWrapper<>();
                        wrapper.eq("name", recipe.getName());
                        Recipe existing = recipeMapper.selectOne(wrapper);

                        if (existing != null) {
                            // 更新已存在的记录
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
                        log.error("解析文件失败: {}", resource.getFilename(), e);
                    }
                }
            } catch (Exception e) {
                log.warn("扫描目录失败: {}", dirName, e);
            }
        }

        log.info("菜谱导入完成: 新增 {} 条, 更新 {} 条, 跳过 {} 条", importCount, updateCount, skipCount);
    }

    /**
     * 检查是否需要更新
     */
    private boolean needsUpdate(Recipe existing, Recipe newRecipe) {
        boolean ingredientsEmpty = StrUtil.isBlank(existing.getIngredients()) || "[]".equals(existing.getIngredients());
        boolean stepsEmpty = StrUtil.isBlank(existing.getSteps()) || "[]".equals(existing.getSteps());
        boolean newIngredientsNotEmpty = StrUtil.isNotBlank(newRecipe.getIngredients()) && !"[]".equals(newRecipe.getIngredients());
        boolean newStepsNotEmpty = StrUtil.isNotBlank(newRecipe.getSteps()) && !"[]".equals(newRecipe.getSteps());

        return (ingredientsEmpty && newIngredientsNotEmpty) || (stepsEmpty && newStepsNotEmpty);
    }

    /**
     * 解析 Markdown 资源
     */
    private Recipe parseMarkdownResource(Resource resource, String category) throws Exception {
        String content;
        try (InputStream is = resource.getInputStream()) {
            content = IoUtil.read(is, StandardCharsets.UTF_8);
        }

        if (StrUtil.isBlank(content)) {
            return null;
        }

        String[] lines = content.split("\n");
        if (lines.length == 0) {
            return null;
        }

        Recipe recipe = new Recipe();

        // 解析菜名
        String name = null;
        for (String line : lines) {
            line = line.trim();
            if (line.startsWith("# ") && !line.startsWith("## ")) {
                name = line.substring(2).trim();
                break;
            }
        }

        if (StrUtil.isBlank(name)) {
            String filename = resource.getFilename();
            name = filename != null ? filename.replace(".md", "") : "未知菜品";
        }
        recipe.setName(name);
        recipe.setCategory(category);
        recipe.setSourceFile(resource.getFilename());

        // 解析图片
        Matcher imageMatcher = IMAGE_PATTERN.matcher(content);
        if (imageMatcher.find()) {
            String imagePath = imageMatcher.group(1);
            recipe.setImageUrl("/" + imagePath);
        }

        // 解析配料和步骤
        List<String> ingredients = new ArrayList<>();
        List<String> steps = new ArrayList<>();
        String currentSection = null;

        for (String line : lines) {
            line = line.trim();

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

            if (line.startsWith("- ") || line.startsWith("* ")) {
                String item = line.substring(2).trim();
                item = item.replaceFirst("^\\d+[.、．]\\s*", "");
                // 移除 markdown 链接格式 [文本](链接) -> 文本
                item = item.replaceAll("\\[([^]]+)]\\([^)]+\\)", "$1");

                if ("ingredients".equals(currentSection) && StrUtil.isNotBlank(item)) {
                    ingredients.add(item);
                } else if ("steps".equals(currentSection) && StrUtil.isNotBlank(item)) {
                    steps.add(item);
                }
            }
        }

        recipe.setIngredients(JSONUtil.toJsonStr(ingredients));
        recipe.setSteps(JSONUtil.toJsonStr(steps));

        // 设置默认值
        recipe.setRecommendScore(3);
        recipe.setSpicyLevel(0);
        recipe.setDifficulty(2);

        return recipe;
    }
}
