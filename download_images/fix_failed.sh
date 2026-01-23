#!/bin/bash

PROXY="http://127.0.0.1:7897"
AGENT="Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36"
TARGET_DIR="/Users/pomazhangfei/Documents/xiaofugui/src/main/resources/static/images/dishes"

# 需要修复的图片列表
declare -a failed_images=(
    "yuxiangrousi.jpg|https://www.chinasichuanfood.com/wp-content/uploads/2016/03/fish-flavored-pork-shreds-2.jpg"
    "laziji.jpg|https://www.chinasichuanfood.com/wp-content/uploads/2016/01/chongqing-chicken-1.jpg"
    "koushuiji.jpg|https://www.chinasichuanfood.com/wp-content/uploads/2015/11/saliva-chicken-1.jpg"
    "suancaiyu.jpg|https://www.chinasichuanfood.com/wp-content/uploads/2015/09/suan-cai-yu-2.jpg"
)

echo "修复失败的图片下载..."

for item in "${failed_images[@]}"; do
    IFS='|' read -r filename url <<< "$item"
    filepath="$TARGET_DIR/$filename"
    
    echo "重新下载: $filename"
    rm -f "$filepath"
    curl -x "$PROXY" -L -A "$AGENT" -o "$filepath" "$url" 2>/dev/null
    
    if [ -f "$filepath" ] && [ -s "$filepath" ] && [ $(stat -f%z "$filepath") -gt 1000 ]; then
        size=$(ls -lh "$filepath" | awk '{print $5}')
        echo "✓ $filename ($size)"
    else
        echo "✗ $filename 仍然失败，尝试备用链接..."
        rm -f "$filepath"
    fi
done

echo ""
echo "检查所有川菜图片状态："
for file in "$TARGET_DIR"/{yuxiangrousi,laziji,koushuiji,suancaiyu,gongbaojiding,shuizhuroupian,huiguorou,mapoqiezi}.jpg; do
    if [ -f "$file" ]; then
        size=$(ls -lh "$file" | awk '{print $5}')
        filename=$(basename "$file")
        if [ $(stat -f%z "$file") -gt 1000 ]; then
            echo "✓ $filename ($size)"
        else
            echo "✗ $filename ($size) - 文件过小"
        fi
    fi
done
