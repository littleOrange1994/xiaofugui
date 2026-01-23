package com.xiaofugui.controller;

import cn.hutool.core.util.StrUtil;
import com.xiaofugui.dto.Result;
import com.xiaofugui.dto.WeeklyPlanAssignRequest;
import com.xiaofugui.dto.WeeklyPlanItemDTO;
import com.xiaofugui.dto.WeeklyPlanRemarkRequest;
import com.xiaofugui.dto.WeeklyPlanRequest;
import com.xiaofugui.entity.Recipe;
import com.xiaofugui.service.WeeklyPlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 本周计划接口
 */
@RestController
@RequestMapping("/api/weekly-plan")
@RequiredArgsConstructor
public class WeeklyPlanController {

    private final WeeklyPlanService weeklyPlanService;

    /**
     * 加入本周计划
     */
    @PostMapping("/add")
    public Result<Void> add(@RequestBody WeeklyPlanRequest request) {
        if (request == null || request.getRecipeId() == null) {
            return Result.error("菜谱ID不能为空");
        }
        try {
            weeklyPlanService.addToWeeklyPlan(request.getRecipeId());
            return Result.success();
        } catch (IllegalArgumentException e) {
            String msg = StrUtil.isBlank(e.getMessage()) ? "参数错误" : e.getMessage();
            return Result.error(msg);
        }
    }

    /**
     * 从本周计划移除
     */
    @PostMapping("/remove")
    public Result<Void> remove(@RequestBody WeeklyPlanRequest request) {
        if (request == null || request.getRecipeId() == null) {
            return Result.error("菜谱ID不能为空");
        }
        try {
            weeklyPlanService.removeFromWeeklyPlan(request.getRecipeId());
            return Result.success();
        } catch (IllegalArgumentException e) {
            String msg = StrUtil.isBlank(e.getMessage()) ? "参数错误" : e.getMessage();
            return Result.error(msg);
        }
    }

    /**
     * 查看本周计划菜品列表
     */
    @GetMapping
    public Result<List<Recipe>> list() {
        return Result.success(weeklyPlanService.listWeeklyPlanRecipes());
    }

    /**
     * 心愿单条目（包含分配周几与备注）
     */
    @GetMapping("/items")
    public Result<List<WeeklyPlanItemDTO>> items() {
        return Result.success(weeklyPlanService.listWeeklyPlanItems());
    }

    /**
     * 判断菜品是否在本周计划中
     */
    @GetMapping("/exists")
    public Result<Boolean> exists(@RequestParam Long recipeId) {
        return Result.success(weeklyPlanService.existsInWeeklyPlan(recipeId));
    }

    /**
     * 本周心愿单数量
     */
    @GetMapping("/count")
    public Result<Long> count() {
        return Result.success(weeklyPlanService.countWeeklyPlan());
    }

    /**
     * 清空本周心愿单
     */
    @PostMapping("/clear")
    public Result<Void> clear() {
        weeklyPlanService.clearWeeklyPlan();
        return Result.success();
    }

    /**
     * 分配到周几
     */
    @PostMapping("/assign")
    public Result<Void> assign(@RequestBody WeeklyPlanAssignRequest request) {
        if (request == null || request.getRecipeId() == null) {
            return Result.error("菜谱ID不能为空");
        }
        try {
            weeklyPlanService.assignPlanDay(request.getRecipeId(), request.getPlanDay());
            return Result.success();
        } catch (IllegalArgumentException e) {
            String msg = StrUtil.isBlank(e.getMessage()) ? "参数错误" : e.getMessage();
            return Result.error(msg);
        }
    }

    /**
     * 更新备注
     */
    @PostMapping("/remark")
    public Result<Void> remark(@RequestBody WeeklyPlanRemarkRequest request) {
        if (request == null || request.getRecipeId() == null) {
            return Result.error("菜谱ID不能为空");
        }
        try {
            weeklyPlanService.updateRemark(request.getRecipeId(), request.getRemark());
            return Result.success();
        } catch (IllegalArgumentException e) {
            String msg = StrUtil.isBlank(e.getMessage()) ? "参数错误" : e.getMessage();
            return Result.error(msg);
        }
    }
}
