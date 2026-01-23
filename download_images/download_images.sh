#!/bin/bash

# 创建代理变量
PROXY="http://127.0.0.1:7897"
AGENT="Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36"

# 川菜图片下载列表 (菜名|下载链接|目标文件名)
declare -a images=(
    "水煮肉片|https://www.chinasichuanfood.com/wp-content/uploads/2015/04/shui-zhu-pork-32.jpg|shuizhuroupian.jpg"
    "回锅肉|https://thewoksoflife.com/wp-content/uploads/2014/11/twice-cooked-pork-6-1.jpg|huiguorou.jpg"
    "宫保鸡丁|https://www.chinasichuanfood.com/wp-content/uploads/2015/01/kung-pao-chicken-1.jpg|gongbaojiding.jpg"
    "鱼香肉丝|https://www.chinasichuanfood.com/wp-content/uploads/2016/03/fish-flavored-pork-shreds-2.jpg|yuxiangrousi.jpg"
    "麻婆豆腐|https://www.chinasichuanfood.com/wp-content/uploads/2015/02/mapo-tofu-1.jpg|mapoqiezi.jpg"
    "辣子鸡|https://www.chinasichuanfood.com/wp-content/uploads/2016/01/chongqing-chicken-1.jpg|laziji.jpg"
    "酸菜鱼|https://www.chinasichuanfood.com/wp-content/uploads/2015/09/suan-cai-yu-2.jpg|suancaiyu.jpg"
    "口水鸡|https://www.chinasichuanfood.com/wp-content/uploads/2015/11/saliva-chicken-1.jpg|koushuiji.jpg"
)

echo "开始下载川菜图片..."
for item in "${images[@]}"; do
    IFS='|' read -r name url filename <<< "$item"
    echo "正在下载: $name -> $filename"
    curl -x "$PROXY" -L -A "$AGENT" -o "$filename" "$url" 2>/dev/null
    if [ -f "$filename" ] && [ -s "$filename" ]; then
        size=$(ls -lh "$filename" | awk '{print $5}')
        echo "✓ 成功: $name ($size)"
    else
        echo "✗ 失败: $name"
    fi
done

echo "下载完成！"
ls -lh *.jpg 2>/dev/null | tail -10
