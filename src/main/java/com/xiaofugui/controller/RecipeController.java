package com.xiaofugui.controller;

import com.xiaofugui.dto.PageResult;
import com.xiaofugui.dto.Result;
import com.xiaofugui.entity.Recipe;
import com.xiaofugui.service.RecipeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 菜谱接口
 */
@RestController
@RequestMapping("/api/recipes")
@RequiredArgsConstructor
public class RecipeController {

    private final RecipeService recipeService;

    /**
     * 分页查询菜谱列表
     */
    @GetMapping
    public Result<PageResult<Recipe>> listRecipes(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "12") Integer pageSize,
            @RequestParam(required = false) String cuisineType) {

        PageResult<Recipe> result = recipeService.listRecipes(page, pageSize, cuisineType);
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
}
