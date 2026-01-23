#!/bin/bash

PROXY="http://127.0.0.1:7897"
AGENT="Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36"
TARGET_DIR="/Users/pomazhangfei/Documents/xiaofugui/src/main/resources/static/images/dishes"

# 创建目标目录
mkdir -p "$TARGET_DIR"

# 川菜图片下载列表 (目标文件名|下载链接)
declare -a images=(
    "gongbaojiding.jpg|https://www.chinasichuanfood.com/wp-content/uploads/2018/10/Kung-Pao-Cauliflower-24.webp"
    "yuxiangrousi.jpg|https://www.chinasichuanfood.com/wp-content/uploads/2016/03/fish-flavored-pork-shreds-2.jpg"
    "shuizhuroupian.jpg|https://www.chinasichuanfood.com/wp-content/uploads/2015/04/shui-zhu-pork-32.jpg"
    "huiguorou.jpg|https://thewoksoflife.com/wp-content/uploads/2014/11/twice-cooked-pork-6-1.jpg"
    "mapoqiezi.jpg|https://www.chinasichuanfood.com/wp-content/uploads/2024/08/Mapo-Tofu-Recipe.webp"
    "suancaiyu.jpg|https://www.chinasichuanfood.com/wp-content/uploads/2015/09/suan-cai-yu-2.jpg"
    "koushuiji.jpg|https://www.chinasichuanfood.com/wp-content/uploads/2015/11/saliva-chicken-1.jpg"
    "laziji.jpg|https://www.chinasichuanfood.com/wp-content/uploads/2016/01/chongqing-chicken-1.jpg"
)

echo "开始并发下载川菜图片到 $TARGET_DIR ..."
echo "共需下载 ${#images[@]} 张图片"
echo ""

# 并发下载函数
download_image() {
    local filename="$1"
    local url="$2"
    local filepath="$TARGET_DIR/$filename"
    
    curl -x "$PROXY" -L -A "$AGENT" -o "$filepath" "$url" 2>/dev/null
    
    if [ -f "$filepath" ] && [ -s "$filepath" ]; then
        size=$(ls -lh "$filepath" | awk '{print $5}')
        echo "✓ $filename ($size)"
    else
        echo "✗ $filename (失败)"
        rm -f "$filepath"
    fi
}

# 并发下载所有图片
for item in "${images[@]}"; do
    IFS='|' read -r filename url <<< "$item"
    download_image "$filename" "$url" &
done

# 等待所有后台任务完成
wait

echo ""
echo "下载完成！"
echo "已保存的图片："
ls -lh "$TARGET_DIR"/*.jpg "$TARGET_DIR"/*.webp 2>/dev/null | awk '{print $9, "(" $5 ")"}'
