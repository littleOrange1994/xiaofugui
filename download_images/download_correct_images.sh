#!/bin/bash

PROXY="http://127.0.0.1:7897"
AGENT="Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36"
TARGET_DIR="/Users/pomazhangfei/Documents/xiaofugui/src/main/resources/static/images/dishes"

# 川菜图片下载列表 (文件名|菜名|下载链接)
# 使用多个可靠的美食网站源
declare -a images=(
    # 回锅肉
    "huiguorou.jpg|回锅肉|https://www.chinasichuanfood.com/wp-content/uploads/2022/10/twice-cooked-pork-belly-17th.webp"
    
    # 宫保鸡丁
    "gongbaojiding.jpg|宫保鸡丁|https://www.chinasichuanfood.com/wp-content/uploads/2023/10/Kung-Pao-Chicken-Recipe-1.jpg"
    
    # 麻婆豆腐
    "mapoqiezi.jpg|麻婆豆腐|https://www.chinasichuanfood.com/wp-content/uploads/2024/08/Mapo-Tofu-Recipe.webp"
    
    # 水煮鱼
    "shuizhuyu.jpg|水煮鱼|https://www.chinasichuanfood.com/wp-content/uploads/2015/04/shui-zhu-yu-1.jpg"
    
    # 水煮肉片
    "shuizhuroupian.jpg|水煮肉片|https://www.chinasichuanfood.com/wp-content/uploads/2015/04/shui-zhu-pork-32.jpg"
    
    # 鱼香肉丝
    "yuxiangrousi.jpg|鱼香肉丝|https://www.chinasichuanfood.com/wp-content/uploads/2016/03/fish-flavored-pork-shreds-2.jpg"
    
    # 酸菜鱼
    "suancaiyu.jpg|酸菜鱼|https://www.chinasichuanfood.com/wp-content/uploads/2015/09/suan-cai-yu-2.jpg"
    
    # 口水鸡
    "koushuiji.jpg|口水鸡|https://www.chinasichuanfood.com/wp-content/uploads/2015/11/saliva-chicken-1.jpg"
)

echo "开始下载川菜图片..."
echo ""

for item in "${images[@]}"; do
    IFS='|' read -r filename dishname url <<< "$item"
    filepath="$TARGET_DIR/$filename"
    
    echo "下载: $dishname -> $filename"
    curl -x "$PROXY" -L -A "$AGENT" -o "$filepath" "$url" 2>/dev/null
    
    if [ -f "$filepath" ] && [ -s "$filepath" ]; then
        size=$(ls -lh "$filepath" | awk '{print $5}')
        filesize=$(stat -f%z "$filepath")
        if [ $filesize -gt 1000 ]; then
            echo "✓ $dishname ($size)"
        else
            echo "✗ $dishname ($size) - 文件过小"
        fi
    else
        echo "✗ $dishname - 下载失败"
    fi
done

echo ""
echo "下载完成！"
