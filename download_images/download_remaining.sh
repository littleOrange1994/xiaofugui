#!/bin/bash

PROXY="http://127.0.0.1:7897"
AGENT="Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36"
TARGET_DIR="/Users/pomazhangfei/Documents/xiaofugui/src/main/resources/static/images/dishes"

# 需要下载的图片列表
declare -a images=(
    "suancaiyu.jpg|https://omnivorescookbook.com/wp-content/uploads/2024/11/241121_Suan-Cai-Yu_1.jpg"
    "koushuiji.jpg|https://omnivorescookbook.com/wp-content/uploads/2024/08/240613_Saliva-Chicken_2.jpg"
    "yuxiangrousi.jpg|https://thewoksoflife.com/wp-content/uploads/2014/11/fish-flavored-pork-shreds-1.jpg"
    "laziji.jpg|https://thewoksoflife.com/wp-content/uploads/2014/11/chongqing-chicken-1.jpg"
)

echo "下载剩余的川菜图片..."

for item in "${images[@]}"; do
    IFS='|' read -r filename url <<< "$item"
    filepath="$TARGET_DIR/$filename"
    
    echo "下载: $filename"
    rm -f "$filepath"
    curl -x "$PROXY" -L -A "$AGENT" -o "$filepath" "$url" 2>/dev/null
    
    if [ -f "$filepath" ] && [ -s "$filepath" ]; then
        size=$(ls -lh "$filepath" | awk '{print $5}')
        filesize=$(stat -f%z "$filepath")
        if [ $filesize -gt 1000 ]; then
            echo "✓ $filename ($size)"
        else
            echo "✗ $filename ($size) - 文件过小"
        fi
    else
        echo "✗ $filename - 下载失败"
    fi
done

echo ""
echo "最终检查所有川菜图片："
ls -lh "$TARGET_DIR"/*.jpg 2>/dev/null | grep -E "(yuxiangrousi|laziji|koushuiji|suancaiyu|gongbaojiding|shuizhuroupian|huiguorou|mapoqiezi)" | awk '{print $9, "(" $5 ")"}'
