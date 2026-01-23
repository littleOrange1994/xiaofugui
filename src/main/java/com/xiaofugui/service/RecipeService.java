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
    public PageResult<Recipe> listRecipes(Integer page, Integer pageSize, String cuisineType) {
        Page<Recipe> pageParam = new Page<>(page, pageSize);
        QueryWrapper<Recipe> wrapper = new QueryWrapper<>();

        if (StrUtil.isNotBlank(cuisineType)) {
            wrapper.eq("cuisine_type", cuisineType);
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
}
