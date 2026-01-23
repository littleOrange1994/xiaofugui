package com.xiaofugui.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.xiaofugui.entity.Recipe;
import com.xiaofugui.entity.WeeklyPlanRecipe;
import com.xiaofugui.mapper.RecipeMapper;
import com.xiaofugui.mapper.WeeklyPlanRecipeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 本周计划服务
 */
@Service
@RequiredArgsConstructor
public class WeeklyPlanService {

    private static final ZoneId ZONE_ID = ZoneId.of("Asia/Shanghai");

    private final WeeklyPlanRecipeMapper weeklyPlanRecipeMapper;
    private final RecipeMapper recipeMapper;

    /**
     * 加入本周计划
     */
    public void addToWeeklyPlan(Long recipeId) {
        if (recipeId == null) {
            throw new IllegalArgumentException("菜谱ID不能为空");
        }

        Recipe recipe = recipeMapper.selectById(recipeId);
        if (recipe == null) {
            throw new IllegalArgumentException("菜谱不存在");
        }

        LocalDateTime weekStartTime = getWeekStartTime();

        QueryWrapper<WeeklyPlanRecipe> existsWrapper = new QueryWrapper<>();
        existsWrapper.eq("week_start_time", weekStartTime)
                .eq("recipe_id", recipeId)
                .eq("deleted", 0);
        Long count = weeklyPlanRecipeMapper.selectCount(existsWrapper);
        if (count != null && count > 0) {
            return;
        }

        WeeklyPlanRecipe entity = new WeeklyPlanRecipe();
        entity.setWeekStartTime(weekStartTime);
        entity.setRecipeId(recipeId);
        entity.setCreateTime(LocalDateTime.now(ZONE_ID));
        entity.setUpdateTime(entity.getCreateTime());
        entity.setDeleted(0);

        try {
            weeklyPlanRecipeMapper.insert(entity);
        } catch (DuplicateKeyException e) {
            // 并发下唯一索引冲突，视为已加入成功
        }
    }

    /**
     * 从本周计划移除
     */
    public boolean removeFromWeeklyPlan(Long recipeId) {
        if (recipeId == null) {
            throw new IllegalArgumentException("菜谱ID不能为空");
        }

        LocalDateTime weekStartTime = getWeekStartTime();
        QueryWrapper<WeeklyPlanRecipe> wrapper = new QueryWrapper<>();
        wrapper.eq("week_start_time", weekStartTime)
                .eq("recipe_id", recipeId)
                .eq("deleted", 0);
        return weeklyPlanRecipeMapper.delete(wrapper) > 0;
    }

    /**
     * 查询本周计划菜品列表
     */
    public List<Recipe> listWeeklyPlanRecipes() {
        LocalDateTime weekStartTime = getWeekStartTime();

        QueryWrapper<WeeklyPlanRecipe> wrapper = new QueryWrapper<>();
        wrapper.eq("week_start_time", weekStartTime)
                .eq("deleted", 0)
                .orderByDesc("create_time")
                .select("recipe_id");

        List<WeeklyPlanRecipe> relations = weeklyPlanRecipeMapper.selectList(wrapper);
        if (relations == null || relations.isEmpty()) {
            return new ArrayList<>();
        }

        List<Long> recipeIds = relations.stream()
                .map(WeeklyPlanRecipe::getRecipeId)
                .filter(id -> id != null)
                .collect(Collectors.toList());
        if (recipeIds.isEmpty()) {
            return new ArrayList<>();
        }

        List<Recipe> recipes = recipeMapper.selectBatchIds(recipeIds);
        if (recipes == null || recipes.isEmpty()) {
            return new ArrayList<>();
        }

        Map<Long, Recipe> recipeMap = recipes.stream()
                .filter(r -> r.getId() != null)
                .collect(Collectors.toMap(Recipe::getId, Function.identity(), (a, b) -> a));

        List<Recipe> ordered = new ArrayList<>();
        for (Long id : recipeIds) {
            Recipe recipe = recipeMap.get(id);
            if (recipe != null) {
                ordered.add(recipe);
            }
        }
        return ordered;
    }

    /**
     * 判断菜品是否在本周计划中
     */
    public boolean existsInWeeklyPlan(Long recipeId) {
        if (recipeId == null) {
            return false;
        }

        LocalDateTime weekStartTime = getWeekStartTime();
        QueryWrapper<WeeklyPlanRecipe> wrapper = new QueryWrapper<>();
        wrapper.eq("week_start_time", weekStartTime)
                .eq("recipe_id", recipeId)
                .eq("deleted", 0);
        Long count = weeklyPlanRecipeMapper.selectCount(wrapper);
        return count != null && count > 0;
    }

    private LocalDateTime getWeekStartTime() {
        LocalDate today = LocalDate.now(ZONE_ID);
        LocalDate monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        return monday.atStartOfDay();
    }
}
