#!/bin/bash

# API 基础URL
API_BASE="http://localhost:8080/api/recipes"

# 川菜菜谱ID和对应的图片文件名映射
declare -A sichuan_recipes=(
    ["220"]="shuizhuyu.jpg"           # 水煮鱼
    ["221"]="shuizhuroupian.jpg"      # 水煮肉片
    ["222"]="huiguorou.jpg"           # 回锅肉
    ["223"]="gongbaojiding.jpg"       # 宫保鸡丁
    ["224"]="yuxiangrousi.jpg"        # 鱼香肉丝
    ["225"]="malaxiangguo.jpg"        # 麻辣香锅
    ["226"]="maoxuewang.jpg"          # 毛血旺
    ["227"]="laziji.jpg"              # 辣子鸡
    ["228"]="fuqifeipian.jpg"         # 夫妻肺片
    ["229"]="koushuiji.jpg"           # 口水鸡
    ["230"]="ganbiansijiodu.jpg"      # 干煸四季豆
    ["231"]="suantaichaorou.jpg"      # 蒜苔炒肉
    ["232"]="qingjiaorusi.jpg"        # 青椒肉丝
    ["233"]="suancaiyu.jpg"           # 酸菜鱼
    ["234"]="mapoqiezi.jpg"           # 麻婆茄子
    ["235"]="ganguotudoupian.jpg"     # 干锅土豆片
    ["236"]="mayishangshu.jpg"        # 蚂蚁上树
    ["237"]="suanlatudousi.jpg"       # 酸辣土豆丝
    ["238"]="doubanjiyu.jpg"          # 豆瓣鲫鱼
    ["239"]="jianjiaoji.jpg"          # 尖椒鸡
)

echo "开始更新川菜菜谱的图片URL..."
echo "共需更新 ${#sichuan_recipes[@]} 道菜"
echo ""

success_count=0
fail_count=0

for recipe_id in "${!sichuan_recipes[@]}"; do
    image_filename="${sichuan_recipes[$recipe_id]}"
    image_url="/images/dishes/$image_filename"
    
    # 构建JSON请求体
    json_data=$(cat <<EOF
{
    "id": $recipe_id,
    "imageUrl": "$image_url"
}
EOF
)
    
    # 发送更新请求
    response=$(curl -s -X POST "$API_BASE/update" \
        -H "Content-Type: application/json" \
        -d "$json_data")
    
    # 检查响应
    if echo "$response" | grep -q '"code":0'; then
        echo "✓ ID $recipe_id: $image_filename"
        ((success_count++))
    else
        echo "✗ ID $recipe_id: $image_filename - 更新失败"
        echo "  响应: $response"
        ((fail_count++))
    fi
done

echo ""
echo "更新完成！"
echo "成功: $success_count, 失败: $fail_count"
