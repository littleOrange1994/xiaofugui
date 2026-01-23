#!/bin/bash
TARGET_DIR="src/main/resources/static/images/dishes"
mkdir -p "$TARGET_DIR"

download_img() {
    name=$1
    url=$2
    echo "⬇️  Downloading $name ..."
    curl -s -L -A "Mozilla/5.0" -o "$TARGET_DIR/$name" "$url"
    
    if [ -f "$TARGET_DIR/$name" ]; then
         size=$(stat -f%z "$TARGET_DIR/$name")
         echo "   ✓ Saved ($size bytes)"
    else
         echo "   ✗ Failed"
    fi
}

download_img "shuizhuyu.jpg" "https://upload.wikimedia.org/wikipedia/commons/e/e4/Sliced_Fish_in_Hot_Chili_Oil.jpg"
download_img "huiguorou.jpg" "https://upload.wikimedia.org/wikipedia/commons/b/b1/Twice-cooked_Pork_%E5%9B%9E%E9%94%85%E8%82%89_%281648213963%29.jpg"
download_img "gongbaojiding.jpg" "https://upload.wikimedia.org/wikipedia/commons/a/a5/KunG_Pao_Chicken.jpg"
download_img "mapoqiezi.jpg" "https://upload.wikimedia.org/wikipedia/commons/e/e1/Yuxiangqiezi.jpg"
download_img "laziji.jpg" "https://upload.wikimedia.org/wikipedia/commons/e/e1/La_Zi_Ji_%28Chicken_with_Chiles%29_%282269517013%29.jpg"

echo "All downloads complete."