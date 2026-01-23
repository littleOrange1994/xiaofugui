package com.xiaofugui.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.xiaofugui.dto.WeeklyPlanItemDTO;
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
    private static final int REMARK_MAX_LENGTH = 200;

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
        List<WeeklyPlanItemDTO> items = listWeeklyPlanItems();
        return items.stream()
                .map(WeeklyPlanItemDTO::getRecipe)
                .filter(r -> r != null)
                .collect(Collectors.toList());
    }

    /**
     * 查询本周心愿单条目（包含分配周几与备注）
     */
    public List<WeeklyPlanItemDTO> listWeeklyPlanItems() {
        LocalDateTime weekStartTime = getWeekStartTime();

        QueryWrapper<WeeklyPlanRecipe> wrapper = new QueryWrapper<>();
        wrapper.eq("week_start_time", weekStartTime)
                .eq("deleted", 0)
                .orderByDesc("create_time")
                .select("recipe_id", "plan_day", "remark");

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

        List<WeeklyPlanItemDTO> ordered = new ArrayList<>();
        for (WeeklyPlanRecipe relation : relations) {
            Long id = relation.getRecipeId();
            Recipe recipe = recipeMap.get(id);
            if (recipe == null) {
                continue;
            }
            WeeklyPlanItemDTO item = new WeeklyPlanItemDTO();
            item.setRecipe(recipe);
            item.setPlanDay(relation.getPlanDay());
            item.setRemark(relation.getRemark());
            ordered.add(item);
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

    /**
     * 本周心愿单数量
     */
    public long countWeeklyPlan() {
        LocalDateTime weekStartTime = getWeekStartTime();
        QueryWrapper<WeeklyPlanRecipe> wrapper = new QueryWrapper<>();
        wrapper.eq("week_start_time", weekStartTime)
                .eq("deleted", 0);
        Long count = weeklyPlanRecipeMapper.selectCount(wrapper);
        return count == null ? 0L : count;
    }

    /**
     * 清空本周心愿单
     */
    public void clearWeeklyPlan() {
        LocalDateTime weekStartTime = getWeekStartTime();
        QueryWrapper<WeeklyPlanRecipe> wrapper = new QueryWrapper<>();
        wrapper.eq("week_start_time", weekStartTime)
                .eq("deleted", 0);
        weeklyPlanRecipeMapper.delete(wrapper);
    }

    /**
     * 分配到周几：1=周一...7=周日，0/NULL 表示取消分配
     */
    public void assignPlanDay(Long recipeId, Integer planDay) {
        if (recipeId == null) {
            throw new IllegalArgumentException("菜谱ID不能为空");
        }

        Integer normalized = normalizePlanDay(planDay);

        LocalDateTime weekStartTime = getWeekStartTime();
        UpdateWrapper<WeeklyPlanRecipe> wrapper = new UpdateWrapper<>();
        wrapper.eq("week_start_time", weekStartTime)
                .eq("recipe_id", recipeId)
                .eq("deleted", 0)
                .set("plan_day", normalized)
                .set("update_time", LocalDateTime.now(ZONE_ID));
        boolean updated = weeklyPlanRecipeMapper.update(null, wrapper) > 0;
        if (!updated) {
            throw new IllegalArgumentException("请先加入玉芳心愿单");
        }
    }

    /**
     * 更新备注（最多 200 字，空字符串表示清空）
     */
    public void updateRemark(Long recipeId, String remark) {
        if (recipeId == null) {
            throw new IllegalArgumentException("菜谱ID不能为空");
        }

        String normalized = normalizeRemark(remark);
        if (normalized != null && normalized.length() > REMARK_MAX_LENGTH) {
            throw new IllegalArgumentException("备注最多200字");
        }

        LocalDateTime weekStartTime = getWeekStartTime();
        UpdateWrapper<WeeklyPlanRecipe> wrapper = new UpdateWrapper<>();
        wrapper.eq("week_start_time", weekStartTime)
                .eq("recipe_id", recipeId)
                .eq("deleted", 0)
                .set("remark", normalized)
                .set("update_time", LocalDateTime.now(ZONE_ID));
        boolean updated = weeklyPlanRecipeMapper.update(null, wrapper) > 0;
        if (!updated) {
            throw new IllegalArgumentException("请先加入玉芳心愿单");
        }
    }

    private LocalDateTime getWeekStartTime() {
        LocalDate today = LocalDate.now(ZONE_ID);
        LocalDate monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        return monday.atStartOfDay();
    }

    private Integer normalizePlanDay(Integer planDay) {
        if (planDay == null || planDay == 0) {
            return null;
        }
        if (planDay < 1 || planDay > 7) {
            throw new IllegalArgumentException("周几参数错误");
        }
        return planDay;
    }

    private String normalizeRemark(String remark) {
        if (remark == null) {
            return null;
        }
        String trimmed = remark.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
