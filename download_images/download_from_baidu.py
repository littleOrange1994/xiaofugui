#!/usr/bin/env python3
# -*- coding: utf-8 -*-

import urllib.request
import urllib.parse
import json
import os
import time
import re
import socket
from urllib.error import URLError, HTTPError

# 代理设置 (默认关闭，如果需要可取消注释)
# proxy_handler = urllib.request.ProxyHandler({'http': 'http://127.0.0.1:7897', 'https': 'http://127.0.0.1:7897'})
# opener = urllib.request.build_opener(proxy_handler)
# urllib.request.install_opener(opener)

# 目标目录 (使用相对路径)
current_dir = os.getcwd()
target_dir = os.path.join(current_dir, "src/main/resources/static/images/dishes")

# 确保目录存在
if not os.path.exists(target_dir):
    os.makedirs(target_dir)

# 川菜列表
sichuan_dishes = [
    ("水煮鱼", "shuizhuyu.jpg"),
    ("水煮肉片", "shuizhuroupian.jpg"),
    ("回锅肉", "huiguorou.jpg"),
    ("宫保鸡丁", "gongbaojiding.jpg"),
    ("鱼香肉丝", "yuxiangrousi.jpg"),
    ("麻辣香锅", "malaxiangguo.jpg"),
    ("毛血旺", "maoxuewang.jpg"),
    ("辣子鸡", "laziji.jpg"),
    ("夫妻肺片", "fuqifeipian.jpg"),
    ("口水鸡", "koushuiji.jpg"),
    ("干煸四季豆", "ganbiansijiodu.jpg"),
    ("蒜苔炒肉", "suantaichaorou.jpg"),
    ("青椒肉丝", "qingjiaorusi.jpg"),
    ("酸菜鱼", "suancaiyu.jpg"),
    ("麻婆茄子", "mapoqiezi.jpg"), # 修正：ID 234 对应麻婆茄子
    ("干锅土豆片", "ganguotudoupian.jpg"),
    ("蚂蚁上树", "mayishangshu.jpg"),
    ("酸辣土豆丝", "suanlatudousi.jpg"),
    ("豆瓣鲫鱼", "doubanjiyu.jpg"),
    ("尖椒鸡", "jianjiaoji.jpg"),
]

def download_image(dish_name, filename):
    """从百度图片下载菜品图片"""
    try:
        # 构建百度图片搜索URL
        search_url = f"https://image.baidu.com/search/index?tn=baiduimage&word={urllib.parse.quote(dish_name)}"
        
        print(f"搜索: {dish_name}")
        
        # 设置User-Agent
        headers = {
            'User-Agent': 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.114 Safari/537.36',
            'Referer': 'https://image.baidu.com/'
        }
        
        req = urllib.request.Request(search_url, headers=headers)
        
        # 获取页面内容
        with urllib.request.urlopen(req, timeout=10) as response:
            html = response.read().decode('utf-8')
            
            # 查找所有的图片URL (objURL)
            pattern = r'"objURL":"([^"]+)"'
            matches = re.findall(pattern, html)
            
            if matches:
                # 尝试前3个链接，防止某些链接失效
                for i, img_url in enumerate(matches[:3]):
                    # 处理加密的百度URL (简单的替换，如果需要)
                    # 这里直接使用提取的URL，大部分是可以访问的
                    if not img_url.startswith('http'):
                        continue
                        
                    print(f"  尝试下载图片 {i+1}: {img_url[:60]}...")
                    
                    try:
                        img_req = urllib.request.Request(img_url, headers=headers)
                        with urllib.request.urlopen(img_req, timeout=10) as img_response:
                            img_data = img_response.read()
                            
                            filepath = os.path.join(target_dir, filename)
                            
                            # 保存图片
                            with open(filepath, 'wb') as f:
                                f.write(img_data)
                            
                            # 检查文件大小
                            file_size = os.path.getsize(filepath)
                            if file_size > 5000: # 大于5KB
                                print(f"  ✓ 下载成功 ({file_size/1024:.1f}KB)")
                                return True
                            else:
                                print(f"  ✗ 文件过小 ({file_size}B)，尝试下一个...")
                    except Exception as e:
                        print(f"  ✗ 下载尝试失败: {str(e)}")
                        continue
                
                print(f"  ✗ 所有尝试均失败")
                return False
            else:
                print(f"  ✗ 未找到图片链接")
                return False
                
    except Exception as e:
        print(f"  ✗ 搜索/请求失败: {str(e)}")
        return False
    
    finally:
        time.sleep(1)

# 开始下载
print(f"目标目录: {target_dir}")
print("开始从百度图片下载川菜图片...")
print("")

success_count = 0
fail_count = 0

for dish_name, filename in sichuan_dishes:
    if download_image(dish_name, filename):
        success_count += 1
    else:
        fail_count += 1

print("")
print(f"下载完成! 成功: {success_count}, 失败: {fail_count}")