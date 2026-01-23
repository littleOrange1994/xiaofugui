# 小富贵菜谱网站

## 项目简介
基于 Spring Boot 3 + MyBatis Plus 的菜谱管理系统，提供菜谱查询、搜索等 RESTful API 接口。

## 技术栈
- **后端框架**: Spring Boot 3.2.1
- **ORM 框架**: MyBatis Plus 3.5.5
- **数据库**: MySQL 8.0
- **工具库**: Hutool 5.8.24, FastJSON 2.0.43

## 项目结构
```
xiaofugui/
├── src/main/java/com/xiaofugui/
│   ├── controller/          # 控制器层
│   │   └── RecipeController.java
│   ├── service/             # 业务服务层
│   │   └── RecipeService.java
│   ├── mapper/              # 数据访问层
│   │   └── RecipeMapper.java
│   ├── entity/              # 实体类
│   │   └── Recipe.java
│   ├── dto/                 # 数据传输对象
│   │   ├── Result.java
│   │   └── PageResult.java
│   ├── enums/               # 枚举
│   │   └── CuisineTypeEnum.java
│   ├── config/              # 配置类
│   │   └── MyBatisPlusConfig.java
│   ├── importer/            # 数据导入工具
│   │   ├── DataImportApplication.java
│   │   ├── MarkdownParser.java
│   │   └── CuisineClassifier.java
│   └── XiaofuguiApplication.java  # 主启动类
├── src/main/resources/
│   ├── application.yml      # 主配置
│   ├── application-dev.yml  # 开发环境配置
│   └── application-prod.yml # 生产环境配置
├── sql/
│   └── schema.sql           # 数据库表结构
└── pom.xml                  # Maven 配置
```

## 数据库配置
- **地址**: 115.191.20.121:3306
- **数据库**: xiaofugui
- **用户名**: root
- **密码**: caicaicai123

## 使用步骤

### 1. 数据库初始化（已完成）
数据库和表结构已创建完成。

### 2. 执行数据导入
在 IDEA 中运行 `DataImportApplication.java`：
- 数据源：`/Users/pomazhangfei/Documents/CookLikeHOC`
- 共 217 个菜谱文件
- 自动解析 Markdown 文件
- 自动分类菜系（基于关键词匹配）
- 插入数据库

### 3. 启动 Web 应用
在 IDEA 中运行 `XiaofuguiApplication.java`，应用将启动在 `http://localhost:8080`

### 4. 测试 API 接口

#### 分页查询菜谱列表
```bash
curl "http://localhost:8080/api/recipes?page=1&pageSize=10"
```

#### 按菜系筛选
```bash
curl "http://localhost:8080/api/recipes?page=1&pageSize=10&cuisineType=SICHUAN"
```

#### 获取菜谱详情
```bash
curl "http://localhost:8080/api/recipes/1"
```

#### 搜索菜谱
```bash
curl "http://localhost:8080/api/recipes/search?keyword=鱼香&page=1&pageSize=10"
```

## API 接口说明

### RecipeController - 菜谱接口

| 接口 | 方法 | 说明 | 参数 |
|------|------|------|------|
| `/api/recipes` | GET | 分页查询菜谱列表 | `page`, `pageSize`, `cuisineType`（可选） |
| `/api/recipes/{id}` | GET | 获取菜谱详情 | 路径参数：`id` |
| `/api/recipes/search` | GET | 搜索菜谱 | `keyword`, `page`, `pageSize` |

### 统一响应格式
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "total": 100,
    "records": [...]
  }
}
```

## 菜系分类

| 菜系代码 | 菜系名称 | 关键词示例 |
|---------|---------|-----------|
| SICHUAN | 川菜 | 麻辣、鱼香、宫保、回锅、水煮、麻婆 |
| SHANDONG | 鲁菜 | 葱烧、糖醋、九转、爆炒、锅塌、大葱 |
| CANTONESE | 粤菜 | 清蒸、白切、烧腊、煲汤、豉汁、蚝油 |
| HUAIYANG | 淮扬菜 | 狮子头、大煮干丝、文思豆腐、清炖、软兜 |
| OTHER | 其他 | 未匹配到关键词的菜品 |

## 打包部署

### 打包为 JAR（在 IDEA 中执行 Maven package）
生成文件：`target/xiaofugui-1.0.0.jar`

### 启动应用
```bash
java -jar target/xiaofugui-1.0.0.jar --spring.profiles.active=prod
```

## 开发规范
- 严格遵守 Java 编码规范
- 禁止使用 `var` 关键字
- 所有路径使用绝对路径
- 数据库时间字段使用 TIMESTAMP 类型
- 枚举类必须以 Enum 结尾

## 作者
小富贵团队
