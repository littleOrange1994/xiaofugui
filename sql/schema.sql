-- 创建数据库
CREATE DATABASE IF NOT EXISTS xiaofugui DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE xiaofugui;

-- 菜谱表
CREATE TABLE IF NOT EXISTS recipe (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    name            VARCHAR(100) NOT NULL COMMENT '菜名',
    cuisine_type    VARCHAR(20) NOT NULL COMMENT '菜系: SICHUAN|SHANDONG|CANTONESE|HUAIYANG|OTHER',
    category        VARCHAR(50) NOT NULL COMMENT '分类: 炒菜|汤|蒸菜|炖菜等',
    ingredients     TEXT NOT NULL COMMENT '配料表（JSON格式）',
    steps           TEXT NOT NULL COMMENT '做法步骤（JSON数组）',
    image_url       VARCHAR(500) COMMENT '菜品图片URL',
    recommend_score TINYINT DEFAULT 3 COMMENT '推荐指数: 1-5星',
    spicy_level     TINYINT DEFAULT 0 COMMENT '辣度: 0-不辣, 1-微辣, 2-中辣, 3-特辣',
    health_benefit  VARCHAR(500) COMMENT '对身体的益处（可为空）',
    market_price    DECIMAL(10,2) COMMENT '市面价格（可为空）',
    season_tags     VARCHAR(100) COMMENT '适合季节（可为空，逗号分隔）',
    festival_tags   VARCHAR(200) COMMENT '适合节日（可为空，逗号分隔）',
    cooking_time    INT COMMENT '烹饪时长（分钟）',
    difficulty      TINYINT DEFAULT 2 COMMENT '难度: 1-简单, 2-中等, 3-困难',
    source_file     VARCHAR(200) COMMENT '数据来源文件路径',
    create_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         TINYINT DEFAULT 0 COMMENT '逻辑删除: 0-未删除, 1-已删除',

    INDEX idx_cuisine_type (cuisine_type),
    INDEX idx_name (name),
    INDEX idx_spicy_level (spicy_level),
    INDEX idx_category (category),
    INDEX idx_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='菜谱表';
