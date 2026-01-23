package com.xiaofugui.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xiaofugui.dto.PageResult;
import com.xiaofugui.entity.Recipe;
import com.xiaofugui.mapper.RecipeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 菜谱服务
 */
@Service
@RequiredArgsConstructor
public class RecipeService {

    private final RecipeMapper recipeMapper;

    /**
     * 分页查询菜谱列表
     */
    public PageResult<Recipe> listRecipes(Integer page, Integer pageSize, String category) {
        Page<Recipe> pageParam = new Page<>(page, pageSize);
        QueryWrapper<Recipe> wrapper = new QueryWrapper<>();

        if (StrUtil.isNotBlank(category)) {
            wrapper.eq("category", category);
        }

        Page<Recipe> result = recipeMapper.selectPage(pageParam, wrapper);
        return new PageResult<>(result.getTotal(), result.getRecords());
    }

    /**
     * 根据 ID 获取菜谱详情
     */
    public Recipe getRecipeById(Long id) {
        return recipeMapper.selectById(id);
    }

    /**
     * 搜索菜谱
     */
    public PageResult<Recipe> searchRecipes(String keyword, Integer page, Integer pageSize) {
        Page<Recipe> pageParam = new Page<>(page, pageSize);
        QueryWrapper<Recipe> wrapper = new QueryWrapper<>();

        if (StrUtil.isNotBlank(keyword)) {
            wrapper.like("name", keyword);
        }

        Page<Recipe> result = recipeMapper.selectPage(pageParam, wrapper);
        return new PageResult<>(result.getTotal(), result.getRecords());
    }

    /**
     * 添加菜品
     */
    public Long addRecipe(Recipe recipe) {
        recipe.setId(null);
        recipeMapper.insert(recipe);
        return recipe.getId();
    }

    /**
     * 修改菜品
     */
    public boolean updateRecipe(Recipe recipe) {
        if (recipe.getId() == null) {
            return false;
        }
        Recipe existing = recipeMapper.selectById(recipe.getId());
        if (existing == null) {
            return false;
        }
        return recipeMapper.updateById(recipe) > 0;
    }
}
