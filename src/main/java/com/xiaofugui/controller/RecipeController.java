package com.xiaofugui.controller;

import com.xiaofugui.dto.AiRecommendResponse;
import com.xiaofugui.dto.PageResult;
import com.xiaofugui.dto.Result;
import com.xiaofugui.entity.Recipe;
import com.xiaofugui.service.AiRecommendService;
import com.xiaofugui.service.RecipeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

/**
 * 菜谱接口
 */
@RestController
@RequestMapping("/api/recipes")
@RequiredArgsConstructor
public class RecipeController {

    private final RecipeService recipeService;
    private final AiRecommendService aiRecommendService;

    /**
     * 分页查询菜谱列表
     */
    @GetMapping
    public Result<PageResult<Recipe>> listRecipes(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "12") Integer pageSize,
            @RequestParam(required = false) String category) {

        PageResult<Recipe> result = recipeService.listRecipes(page, pageSize, category);
        return Result.success(result);
    }

    /**
     * 分页查询推荐菜谱列表
     */
    @GetMapping("/recommended")
    public Result<PageResult<Recipe>> listRecommended(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "12") Integer pageSize) {
        PageResult<Recipe> result = recipeService.listRecommended(page, pageSize);
        return Result.success(result);
    }

    /**
     * 获取菜谱详情
     */
    @GetMapping("/{id}")
    public Result<Recipe> getRecipeById(@PathVariable Long id) {
        Recipe recipe = recipeService.getRecipeById(id);
        if (recipe == null) {
            return Result.error("菜谱不存在");
        }
        return Result.success(recipe);
    }

    /**
     * 搜索菜谱
     */
    @GetMapping("/search")
    public Result<PageResult<Recipe>> searchRecipes(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "12") Integer pageSize) {

        PageResult<Recipe> result = recipeService.searchRecipes(keyword, page, pageSize);
        return Result.success(result);
    }

    /**
     * 添加菜品
     */
    @PostMapping
    public Result<Long> addRecipe(@RequestBody Recipe recipe) {
        Long id = recipeService.addRecipe(recipe);
        return Result.success(id);
    }

    /**
     * 修改菜品
     */
    @PostMapping("/update")
    public Result<Void> updateRecipe(@RequestBody Recipe recipe) {
        if (recipe.getId() == null) {
            return Result.error("菜品ID不能为空");
        }
        boolean success = recipeService.updateRecipe(recipe);
        if (!success) {
            return Result.error("菜品不存在或更新失败");
        }
        return Result.success();
    }

    /**
     * 删除菜品
     */
    @PostMapping("/delete/{id}")
    public Result<Void> deleteRecipe(@PathVariable Long id) {
        boolean success = recipeService.deleteRecipe(id);
        if (!success) {
            return Result.error("菜品不存在或删除失败");
        }
        return Result.success();
    }

    /**
     * AI 智能推荐菜品
     */
    @PostMapping("/ai-recommend")
    public Result<AiRecommendResponse> aiRecommend() {
        AiRecommendResponse response = aiRecommendService.recommend();
        return Result.success(response);
    }

    /**
     * AI 搜索 - 根据关键词生成烹饪说明（SSE 流式返回）
     */
    @GetMapping(value = "/ai-search", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> aiSearchStream(@RequestParam String keyword) {
        return aiRecommendService.aiSearchStream(keyword);
    }
}
