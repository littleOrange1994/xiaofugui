package com.xiaofugui.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xiaofugui.entity.Recipe;
import org.apache.ibatis.annotations.Mapper;

/**
 * 菜谱 Mapper
 */
@Mapper
public interface RecipeMapper extends BaseMapper<Recipe> {
}
