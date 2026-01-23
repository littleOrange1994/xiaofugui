package com.xiaofugui.enums;

import lombok.Getter;

/**
 * 菜系类型枚举
 */
@Getter
public enum CuisineTypeEnum {

    SICHUAN("SICHUAN", "川菜"),
    SHANDONG("SHANDONG", "鲁菜"),
    CANTONESE("CANTONESE", "粤菜"),
    HUAIYANG("HUAIYANG", "淮扬菜"),
    OTHER("OTHER", "其他");

    private final String code;
    private final String name;

    CuisineTypeEnum(String code, String name) {
        this.code = code;
        this.name = name;
    }
}
