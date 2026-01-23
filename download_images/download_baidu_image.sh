#!/bin/bash

PROXY="http://127.0.0.1:7897"
AGENT="Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36"
TARGET_DIR="/Users/pomazhangfei/Documents/xiaofugui/src/main/resources/static/images/dishes"

# 百度百科图片URL
baidu_url="https://baike.baidu.com/pic/%E6%B0%B4%E7%85%AE%E9%B1%BC/30972/1/11385343fbf2b211931330b301ca72380cd7902320b1"

echo "尝试从百度百科下载水煮鱼图片..."
echo ""

# 方法1: 直接下载
echo "方法1: 直接下载百度百科图片..."
curl -x "$PROXY" -L -A "$AGENT" \
  -H "Referer: https://baike.baidu.com/" \
  -H "Accept: image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8" \
  -o "$TARGET_DIR/shuizhuyu_baidu.jpg" \
  "$baidu_url" 2>/dev/null

if [ -f "$TARGET_DIR/shuizhuyu_baidu.jpg" ]; then
    size=$(ls -lh "$TARGET_DIR/shuizhuyu_baidu.jpg" | awk '{print $5}')
    filesize=$(stat -f%z "$TARGET_DIR/shuizhuyu_baidu.jpg")
    if [ $filesize -gt 1000 ]; then
        echo "✓ 下载成功 ($size)"
        cp "$TARGET_DIR/shuizhuyu_baidu.jpg" "$TARGET_DIR/shuizhuyu.jpg"
        echo "✓ 已保存为 shuizhuyu.jpg"
    else
        echo "✗ 文件过小 ($size)"
    fi
else
    echo "✗ 下载失败"
fi

# 方法2: 尝试从百度图片CDN下载
echo ""
echo "方法2: 尝试从百度图片CDN下载..."

# 百度图片的常见CDN URL格式
cdn_urls=(
    "https://imgsrc.baidu.com/forum/pic/item/11385343fbf2b211931330b301ca72380cd7902320b1.jpg"
    "https://pic.baidu.com/feed/11385343fbf2b211931330b301ca72380cd7902320b1.jpg"
    "https://bkimg.cdn.bcebos.com/pic/11385343fbf2b211931330b301ca72380cd7902320b1"
)

for cdn_url in "${cdn_urls[@]}"; do
    echo "尝试: $cdn_url"
    curl -x "$PROXY" -L -A "$AGENT" \
      -H "Referer: https://baike.baidu.com/" \
      -o "$TARGET_DIR/shuizhuyu_test.jpg" \
      "$cdn_url" 2>/dev/null
    
    if [ -f "$TARGET_DIR/shuizhuyu_test.jpg" ]; then
        filesize=$(stat -f%z "$TARGET_DIR/shuizhuyu_test.jpg")
        if [ $filesize -gt 1000 ]; then
            size=$(ls -lh "$TARGET_DIR/shuizhuyu_test.jpg" | awk '{print $5}')
            echo "✓ 下载成功 ($size)"
            cp "$TARGET_DIR/shuizhuyu_test.jpg" "$TARGET_DIR/shuizhuyu.jpg"
            echo "✓ 已保存为 shuizhuyu.jpg"
            break
        fi
    fi
done

echo ""
echo "最终检查:"
if [ -f "$TARGET_DIR/shuizhuyu.jpg" ]; then
    size=$(ls -lh "$TARGET_DIR/shuizhuyu.jpg" | awk '{print $5}')
    filesize=$(stat -f%z "$TARGET_DIR/shuizhuyu.jpg")
    if [ $filesize -gt 1000 ]; then
        echo "✓ 水煮鱼图片已成功下载 ($size)"
    else
        echo "✗ 文件过小"
    fi
fi
