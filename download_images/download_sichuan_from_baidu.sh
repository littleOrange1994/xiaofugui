#!/bin/bash

PROXY="http://127.0.0.1:7897"
AGENT="Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
TARGET_DIR="/Users/pomazhangfei/Documents/xiaofugui/src/main/resources/static/images/dishes"

# 川菜图片映射 (菜名|百度百科ID|目标文件)
declare -a sichuan_images=(
    "水煮鱼|30972|shuizhuyu.jpg"
    "回锅肉|254670|huiguorou.jpg"
    "宫保鸡丁|1048|gongbaojiding.jpg"
    "麻婆豆腐|1048|mapoqiezi.jpg"
    "辣子鸡|1048|laziji.jpg"
)

echo "从百度百科下载川菜高质量图片..."
echo ""

for item in "${sichuan_images[@]}"; do
    IFS='|' read -r dish_name baidu_id target_file <<< "$item"
    
    echo "下载: $dish_name"
    
    # 尝试多个百度CDN URL格式
    urls=(
        "https://bkimg.cdn.bcebos.com/pic/11385343fbf2b211931330b301ca72380cd7902320b1"
        "https://imgsrc.baidu.com/baike/pic/item/11385343fbf2b211931330b301ca72380cd7902320b1.jpg"
        "https://pic.baidu.com/feed/11385343fbf2b211931330b301ca72380cd7902320b1.jpg"
    )
    
    for url in "${urls[@]}"; do
        curl -x "$PROXY" -L -A "$AGENT" \
          -H "Referer: https://baike.baidu.com/" \
          -o "$TARGET_DIR/$target_file" \
          "$url" 2>/dev/null
        
        if [ -f "$TARGET_DIR/$target_file" ]; then
            filesize=$(stat -f%z "$TARGET_DIR/$target_file")
            if [ $filesize -gt 5000 ]; then
                size=$(ls -lh "$TARGET_DIR/$target_file" | awk '{print $5}')
                echo "  ✓ 下载成功 ($size)"
                break
            fi
        fi
    done
done

echo ""
echo "检查下载结果:"
ls -lh "$TARGET_DIR"/{shuizhuyu,huiguorou,gongbaojiding,mapoqiezi,laziji}.jpg 2>/dev/null | awk '{print $9, "(" $5 ")"}'
