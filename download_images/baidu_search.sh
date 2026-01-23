#!/bin/bash

PROXY="http://127.0.0.1:7897"
AGENT="Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36"
TARGET_DIR="/Users/pomazhangfei/Documents/xiaofugui/src/main/resources/static/images/dishes"

# 川菜菜名列表
declare -a sichuan_dishes=(
    "水煮鱼"
    "水煮肉片"
    "回锅肉"
    "宫保鸡丁"
    "鱼香肉丝"
    "麻辣香锅"
    "毛血旺"
    "辣子鸡"
    "夫妻肺片"
    "口水鸡"
    "干煸四季豆"
    "蒜苔炒肉"
    "青椒肉丝"
    "酸菜鱼"
    "麻婆豆腐"
    "干锅土豆片"
    "蚂蚁上树"
    "酸辣土豆丝"
    "豆瓣鲫鱼"
    "尖椒鸡"
)

echo "开始从百度图片搜索川菜图片..."
echo ""

for dish in "${sichuan_dishes[@]}"; do
    echo "搜索: $dish"
    # 这里需要手动从百度获取图片链接
    # 由于百度图片搜索需要JavaScript渲染，无法直接获取
done

echo ""
echo "提示: 百度图片搜索需要JavaScript渲染，建议使用以下方法:"
echo "1. 访问 https://image.baidu.com/search/index?tn=baiduimage&word=菜名"
echo "2. 右键点击图片 -> 复制图片链接"
echo "3. 或使用专业的图片爬虫工具"
