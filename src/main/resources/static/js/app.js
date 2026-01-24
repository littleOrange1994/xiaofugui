// 全局状态
const state = {
    recipes: [],
    currentPage: 1,
    pageSize: 12,
    total: 0,
    category: '',
    searchKeyword: ''
};

// API 基础路径
const API_BASE = '/api/recipes';
const WEEKLY_PLAN_API_BASE = '/api/weekly-plan';

// 页面加载时初始化
document.addEventListener('DOMContentLoaded', function() {
    // 先恢复从详情页返回时的状态
    restoreStateFromHash();

    initFilterTags();
    initSearchInput();
    loadRecipes();
    loadWeeklyPlanCount();

    // 优化性能：滚动时暂停飘浮动画
    optimizeFloatingAnimation();
});

/**
 * 从URL hash中恢复页面状态
 * 用于从详情页返回时恢复之前的页码、分类、搜索关键词
 */
function restoreStateFromHash() {
    const hash = window.location.hash;
    if (!hash || hash === '#') {
        return;
    }

    // 去掉 # 号，解析参数
    const params = new URLSearchParams(hash.substring(1));
    const page = params.get('page');
    const category = params.get('category');
    const search = params.get('search');

    // 恢复状态
    if (page) {
        state.currentPage = parseInt(page);
    }
    if (category) {
        state.category = category;
        // 激活对应的分类标签
        const tags = document.querySelectorAll('.filter-tag');
        tags.forEach(tag => {
            if (tag.dataset.category === category) {
                tag.classList.add('active');
            } else {
                tag.classList.remove('active');
            }
        });
    }
    if (search) {
        state.searchKeyword = search;
        const searchInput = document.getElementById('searchInput');
        if (searchInput) {
            searchInput.value = search;
        }
    }

    // 清除hash避免刷新时重复恢复
    window.history.replaceState(null, '', window.location.pathname);
}

// 初始化筛选标签点击事件
function initFilterTags() {
    const tags = document.querySelectorAll('.filter-tag');
    tags.forEach(tag => {
        tag.addEventListener('click', function() {
            // 移除所有 active 状态
            tags.forEach(t => t.classList.remove('active'));
            // 移除推荐按钮的 active 状态
            const recommendBtn = document.querySelector('.special-recommend-btn');
            if (recommendBtn) {
                recommendBtn.classList.remove('active');
            }
            // 添加当前 active 状态
            this.classList.add('active');
            // 更新筛选条件
            state.category = this.dataset.category;
            state.currentPage = 1;
            state.searchKeyword = '';
            document.getElementById('searchInput').value = '';
            loadRecipes();
        });
    });
}

// 初始化搜索输入框回车事件
function initSearchInput() {
    const input = document.getElementById('searchInput');
    input.addEventListener('keypress', function(e) {
        if (e.key === 'Enter') {
            searchRecipes();
        }
    });
}

// 搜索菜谱
function searchRecipes() {
    const keyword = document.getElementById('searchInput').value.trim();
    if (!keyword) {
        // 清空搜索时，恢复默认列表
        state.searchKeyword = '';
        state.currentPage = 1;
        loadRecipes();
        return;
    }

    state.searchKeyword = keyword;
    state.category = '';
    state.currentPage = 1;

    // 重置筛选标签状态
    const tags = document.querySelectorAll('.filter-tag');
    tags.forEach(t => t.classList.remove('active'));
    document.querySelector('.filter-tag[data-category=""]').classList.add('active');

    loadRecipes();
}

// 加载菜谱列表
async function loadRecipes() {
    showLoading(true);

    try {
        let url;
        if (state.searchKeyword) {
            // 搜索模式
            url = `${API_BASE}/search?keyword=${encodeURIComponent(state.searchKeyword)}&page=${state.currentPage}&pageSize=${state.pageSize}`;
        } else if (state.category === '推荐') {
            // 推荐模式
            url = `${API_BASE}/recommended?page=${state.currentPage}&pageSize=${state.pageSize}`;
        } else {
            // 列表模式
            url = `${API_BASE}?page=${state.currentPage}&pageSize=${state.pageSize}`;
            if (state.category) {
                url += `&category=${encodeURIComponent(state.category)}`;
            }
        }

        const response = await fetch(url);
        const result = await response.json();

        if (result.code === 0 && result.data) {
            state.recipes = result.data.records || [];
            state.total = result.data.total || 0;
            renderRecipes();
            renderPagination();
        } else {
            showError(result.message || '加载失败');
        }
    } catch (error) {
        console.error('加载菜谱失败:', error);
        showError('网络错误，请稍后重试');
    } finally {
        showLoading(false);
    }
}

// 渲染菜谱卡片
function renderRecipes() {
    const grid = document.getElementById('recipeGrid');
    const emptyState = document.getElementById('emptyState');

    if (state.recipes.length === 0) {
        grid.innerHTML = '';
        // 如果是搜索模式且没有结果，自动触发 AI 搜索
        if (state.searchKeyword) {
            emptyState.style.display = 'none';
            showAiSearch(state.searchKeyword);
        } else {
            emptyState.style.display = 'block';
        }
        return;
    }

    emptyState.style.display = 'none';

    grid.innerHTML = state.recipes.map(recipe => `
        <div class="recipe-card" onclick="viewRecipe(${recipe.id})">
            <div class="card-image-wrapper">
                ${recipe.imageUrl
                    ? `<img src="${recipe.imageUrl}" alt="${recipe.name}" loading="lazy" onerror="this.outerHTML='<div class=\\'placeholder-img\\'>🍲</div>'">`
                    : '<div class="placeholder-img">🍲</div>'
                }
                <div class="card-overlay">
                    <span class="overlay-text">👀 查看详情</span>
                </div>
            </div>
            <div class="recipe-card-content">
                <div class="recipe-card-title">
                    <h3>${escapeHtml(recipe.name)}</h3>
                    <button class="card-ai-btn" onclick="event.stopPropagation(); goToDetailWithAI(${recipe.id})">🤖 AI一下</button>
                </div>
                <div class="recipe-card-meta">
                    ${recipe.category ? `<span class="meta-tag cuisine">${escapeHtml(recipe.category)}</span>` : ''}
                    ${recipe.difficulty ? `<span class="meta-tag difficulty">${getDifficultyText(recipe.difficulty)}</span>` : ''}
                </div>
                <div class="recipe-card-footer">
                    <span class="spicy-level">${getSpicyLevel(recipe.spicyLevel)}</span>
                    <span class="recommend-score">${getRecommendScore(recipe.recommendScore)}</span>
                </div>
            </div>
        </div>
    `).join('');
}

// 渲染分页
function renderPagination() {
    const pagination = document.getElementById('pagination');
    const totalPages = Math.ceil(state.total / state.pageSize);

    if (totalPages <= 1) {
        pagination.style.display = 'none';
        return;
    }

    pagination.style.display = 'flex';
    document.getElementById('prevBtn').disabled = state.currentPage <= 1;
    document.getElementById('nextBtn').disabled = state.currentPage >= totalPages;
    document.getElementById('pageInfo').textContent = `第 ${state.currentPage} / ${totalPages} 页，共 ${state.total} 道菜`;
}

// 切换页码
function changePage(delta) {
    const totalPages = Math.ceil(state.total / state.pageSize);
    const newPage = state.currentPage + delta;

    if (newPage < 1 || newPage > totalPages) {
        return;
    }

    state.currentPage = newPage;
    loadRecipes();

    // 滚动到顶部
    window.scrollTo({ top: 0, behavior: 'smooth' });
}

// 查看菜谱详情
function viewRecipe(id) {
    // 保存当前页面状态到URL参数，方便返回时恢复
    const params = new URLSearchParams();
    params.set('id', id);
    params.set('page', state.currentPage);
    if (state.category) {
        params.set('category', state.category);
    }
    if (state.searchKeyword) {
        params.set('search', state.searchKeyword);
    }
    window.location.href = `detail.html?${params.toString()}`;
}

// 跳转到详情页并自动触发 AI
function goToDetailWithAI(id) {
    const params = new URLSearchParams();
    params.set('id', id);
    params.set('page', state.currentPage);
    params.set('autoAI', 'true');
    if (state.category) {
        params.set('category', state.category);
    }
    if (state.searchKeyword) {
        params.set('search', state.searchKeyword);
    }
    window.location.href = `detail.html?${params.toString()}`;
}

// 查看玉芳心愿单
function viewWeeklyPlan() {
    window.location.href = 'week-plan.html';
}

// 加载玉芳心愿单数量
async function loadWeeklyPlanCount() {
    const btn = document.getElementById('weeklyPlanHomeBtn');
    if (!btn) return;

    try {
        const response = await fetch(`${WEEKLY_PLAN_API_BASE}/count`);
        const result = await response.json();
        if (result.code === 0) {
            const count = Number(result.data || 0);
            btn.textContent = count > 0 ? `📅 玉芳心愿单(${count})` : '📅 玉芳心愿单';
        }
    } catch (error) {
        console.error('加载玉芳心愿单数量失败:', error);
    }
}

// 显示/隐藏加载状态
function showLoading(show) {
    const loading = document.getElementById('loading');
    const grid = document.getElementById('recipeGrid');

    if (show) {
        loading.style.display = 'block';
        grid.style.opacity = '0.5';
    } else {
        loading.style.display = 'none';
        grid.style.opacity = '1';
    }
}

// 显示错误信息
function showError(message) {
    const grid = document.getElementById('recipeGrid');
    grid.innerHTML = `
        <div class="empty-state" style="grid-column: 1/-1;">
            <div class="icon">😥</div>
            <p>${escapeHtml(message)}</p>
        </div>
    `;
}

// 获取难度文字
function getDifficultyText(level) {
    const texts = ['', '简单', '普通', '较难', '困难', '大师级'];
    return texts[level] || '未知';
}

// 获取辣度显示
function getSpicyLevel(level) {
    if (!level || level === 0) {
        return '<span class="spicy-none">🌶️ 不辣</span>';
    }
    const peppers = '🌶️'.repeat(Math.min(level, 5));
    const levelText = ['', '微辣', '中辣', '较辣', '重辣', '变态辣'][Math.min(level, 5)];
    return `<span class="spicy-hot">${peppers} ${levelText}</span>`;
}

// 获取推荐星级
function getRecommendScore(score) {
    if (!score) return '<span class="stars">☆☆☆☆☆</span>';
    // score 范围 1-10，转换为 1-5 星
    const rating = Math.min(5, Math.max(1, Math.round(score / 2)));
    const fullStars = '★'.repeat(rating);
    const emptyStars = '☆'.repeat(5 - rating);
    return `<span class="stars">${fullStars}${emptyStars}</span> <span class="score-num">${score}/10</span>`;
}

// HTML 转义
function escapeHtml(text) {
    if (!text) return '';
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

// ========== 今天吃什么功能 ==========

// 季节文案库
const SEASON_TIPS = {
    spring: [
        '春眠不觉晓，吃饱才能跑 🏃 万物复苏的季节，胃也该醒醒了 😋',
        '春风十里不如你做的饭香 🌷 减肥是不可能的，这辈子都不可能 🤤',
        '春天来了，肉肉藏不住了 😱 但先吃饱再说，毕竟饿着没力气减肥 💪',
        '春暖花开，食欲大开 🌸 不吃饱哪有力气欣赏春光 🦋',
        '一年之计在于春，一日之计在于吃 🍽️ 干饭人永不认输 ✊'
    ],
    summer: [
        '热到和太阳称兄道弟 🔥 命都是空调和冰镇西瓜给的 🍉',
        '出门五分钟，流汗两小时 💦 这种天气只想躺着吃 🛋️',
        '热得像红烧肉 🥵 但该吃还得吃，毕竟是个敬业的干饭人 🍚',
        '夏天的命是冰奶茶给的 🧋 当然还有各种好吃的续命 😎',
        '三伏天热成狗 🐕 只有美食能拯救我这条咸鱼 🐟'
    ],
    autumn: [
        '秋风起，贴膘忙 🐷 瘦了一夏天，该把肉补回来了 💪',
        '秋天的第一杯奶茶喝了，第一顿大餐也得安排上 🍂',
        '天凉好个秋，胃口大如牛 🐂 不吃对不起这凉爽的天气 🍁',
        '秋高气爽，最适合干饭 🌾 毕竟冬天还要靠这身膘过冬 ❄️',
        '都说秋天是收获的季节 🎃 那我就收获一下体重吧 😂'
    ],
    winter: [
        '外面冷得像冰箱 🥶 不吃点热乎的怎么对得起这哆嗦的身体 🍲',
        '冬天就是要吃肉肉 🍖 脂肪是对寒冷最大的尊重 🧣',
        '瑟瑟发抖的季节 ❄️ 只有热气腾腾的美食能温暖我 🫕',
        '冬天不长膘，来年春天跑不掉 🏃 先吃为敬 🙏',
        '北风那个吹，雪花那个飘 ☃️ 躲在家里吃火锅才是正经事 🔥'
    ]
};

// 获取当前季节
function getCurrentSeason() {
    const month = new Date().getMonth() + 1;
    let name, tips;
    if (month >= 3 && month <= 5) {
        name = '🌸 春季';
        tips = SEASON_TIPS.spring;
    } else if (month >= 6 && month <= 8) {
        name = '☀️ 夏季';
        tips = SEASON_TIPS.summer;
    } else if (month >= 9 && month <= 11) {
        name = '🍂 秋季';
        tips = SEASON_TIPS.autumn;
    } else {
        name = '❄️ 冬季';
        tips = SEASON_TIPS.winter;
    }
    // 随机选择一条文案
    const tip = tips[Math.floor(Math.random() * tips.length)];
    return { name, tip };
}

// 显示 AI 推荐菜谱
async function showRandomRecipe() {
    const modal = document.getElementById('randomModal');
    const preview = document.getElementById('randomRecipePreview');
    const seasonTip = document.getElementById('seasonTip');
    const viewBtn = document.getElementById('viewRandomBtn');

    // 显示 AI 加载状态，禁用查看详情按钮，保持和结果一致的布局
    preview.innerHTML = `
        <div class="ai-loading-placeholder">
            <div class="loading-food-slot" id="foodSlot">🍜</div>
        </div>
        <h3>AI 正在为您精选...</h3>
        <p style="color:#888;margin-top:10px;">稍等片刻，美味即将揭晓 ✨</p>
        <div class="ai-reason-card ai-reason-loading">
            <span class="ai-reason-icon">🤖</span>
            <span class="ai-reason-text">正在思考推荐理由中...</span>
        </div>
    `;
    viewBtn.disabled = true;
    viewBtn.onclick = null;
    modal.classList.add('show');

    // 老虎机滚动食物图标
    const foodEmojis = ['🍜', '🍲', '🍖', '🍗', '🥘', '🍛', '🍝', '🥗', '🍳', '🥩', '🍤', '🦐', '🐟', '🥟', '🫕'];
    const foodSlot = document.getElementById('foodSlot');
    const slotInterval = setInterval(() => {
        if (foodSlot) {
            foodSlot.textContent = foodEmojis[Math.floor(Math.random() * foodEmojis.length)];
        }
    }, 150);

    // 先显示通用季节提示
    const season = getCurrentSeason();
    seasonTip.textContent = `${season.name}推荐 · ${season.tip}`;

    try {
        // 调用 AI 推荐接口
        const response = await fetch(`${API_BASE}/ai-recommend`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' }
        });
        const result = await response.json();

        // 停止老虎机动画
        clearInterval(slotInterval);

        if (result.code === 0 && result.data && result.data.recipe) {
            const { recipe, reason } = result.data;

            // 渲染推荐菜品
            preview.innerHTML = `
                ${recipe.imageUrl
                    ? `<img src="${recipe.imageUrl}" alt="${recipe.name}" loading="lazy" onerror="this.src='data:image/svg+xml,<svg xmlns=%22http://www.w3.org/2000/svg%22 viewBox=%220 0 100 100%22><text y=%22.9em%22 font-size=%2280%22>🍲</text></svg>'">`
                    : '<div style="width:150px;height:150px;background:#fff5f5;border-radius:10px;display:flex;align-items:center;justify-content:center;font-size:4rem;margin:0 auto 15px;">🍲</div>'
                }
                <h3>${escapeHtml(recipe.name)}</h3>
                <p style="color:#888;margin-top:10px;">
                    ${recipe.category || ''} · ${getDifficultyText(recipe.difficulty)} · ${getSpicyLevel(recipe.spicyLevel)}
                </p>
                ${reason ? `<div class="ai-reason-card"><span class="ai-reason-icon">🤖</span><span class="ai-reason-text">${escapeHtml(reason)}</span></div>` : ''}
            `;

            // 设置查看详情按钮并启用
            viewBtn.disabled = false;
            viewBtn.onclick = () => {
                viewRecipe(recipe.id);
            };
        } else {
            preview.innerHTML = '<div style="padding: 40px; color: #999;">暂无推荐，请稍后再试</div>';
            viewBtn.disabled = true;
        }
    } catch (error) {
        clearInterval(slotInterval);
        console.error('获取 AI 推荐失败:', error);
        preview.innerHTML = '<div style="padding: 40px; color: #999;">获取推荐失败，请稍后再试</div>';
        viewBtn.disabled = true;
    }
}

// 关闭随机推荐弹窗
function closeRandomModal(event) {
    if (event.target.id === 'randomModal') {
        document.getElementById('randomModal').classList.remove('show');
    }
}

// ========== 小曾推荐功能 ==========

// 显示小曾推荐分类
function showRecommendCategory() {
    console.log('小曾推荐被点击'); // 调试日志

    // 重置筛选标签状态
    const tags = document.querySelectorAll('.filter-tag');
    tags.forEach(t => t.classList.remove('active'));

    // 激活推荐按钮
    const recommendBtn = document.querySelector('.special-recommend-btn');
    if (recommendBtn) {
        recommendBtn.classList.add('active');
    }

    // 清空搜索
    document.getElementById('searchInput').value = '';

    // 设置状态并加载
    state.category = '推荐';
    state.searchKeyword = '';
    state.currentPage = 1;
    loadRecipes();

    // 滚动到菜谱列表
    setTimeout(() => {
        document.getElementById('recipeGrid').scrollIntoView({ behavior: 'smooth', block: 'start' });
    }, 100);
}

/**
 * 优化飘浮动画性能
 * 用户滚动时暂停动画，停止滚动后恢复
 */
function optimizeFloatingAnimation() {
    const floatingBg = document.querySelector('.floating-food-bg');
    if (!floatingBg) return;

    let scrollTimer = null;

    window.addEventListener('scroll', function() {
        // 滚动时暂停动画
        floatingBg.style.animationPlayState = 'paused';
        floatingBg.querySelectorAll('.food-item').forEach(item => {
            item.style.animationPlayState = 'paused';
        });

        // 清除之前的定时器
        if (scrollTimer) {
            clearTimeout(scrollTimer);
        }

        // 停止滚动300ms后恢复动画
        scrollTimer = setTimeout(function() {
            floatingBg.style.animationPlayState = 'running';
            floatingBg.querySelectorAll('.food-item').forEach(item => {
                item.style.animationPlayState = 'running';
            });
        }, 300);
    }, { passive: true });
}

// ========== AI 搜索功能 ==========

// 当前 AI 搜索的 EventSource 实例
let currentAiSearchEventSource = null;
// 当前 AI 搜索的原始内容（用于复制）
let currentAiSearchContent = '';

/**
 * 显示 AI 搜索弹窗并调用 AI 生成烹饪说明（SSE 流式）
 */
function showAiSearch(keyword) {
    const modal = document.getElementById('aiSearchModal');
    const keywordEl = document.getElementById('aiSearchKeyword');
    const contentEl = document.getElementById('aiSearchContent');

    // 关闭之前的连接
    if (currentAiSearchEventSource) {
        currentAiSearchEventSource.close();
        currentAiSearchEventSource = null;
    }

    // 重置内容
    currentAiSearchContent = '';

    // 显示弹窗和加载状态
    keywordEl.textContent = `正在为您生成「${keyword}」的烹饪说明...`;
    contentEl.innerHTML = `
        <div class="ai-search-loading">
            <div class="ai-search-loading-icon">🍳</div>
            <p>AI 正在思考中...</p>
        </div>
    `;
    modal.classList.add('show');

    // 使用 EventSource 接收 SSE 流
    let fullContent = '';
    const eventSource = new EventSource(`${API_BASE}/ai-search?keyword=${encodeURIComponent(keyword)}`);
    currentAiSearchEventSource = eventSource;

    eventSource.onmessage = function(event) {
        // 更新标题
        if (fullContent === '') {
            keywordEl.textContent = `「${keyword}」的烹饪说明`;
        }

        // 累积内容
        fullContent += event.data;
        currentAiSearchContent = fullContent;

        // 实时渲染 Markdown
        contentEl.innerHTML = `<div class="ai-search-result">${renderMarkdown(fullContent)}<span class="ai-cursor">▌</span></div>`;

        // 滚动到底部
        contentEl.scrollTop = contentEl.scrollHeight;
    };

    eventSource.onerror = function(error) {
        eventSource.close();
        currentAiSearchEventSource = null;

        if (fullContent) {
            // 已有内容，移除光标
            contentEl.innerHTML = `<div class="ai-search-result">${renderMarkdown(fullContent)}</div>`;
        } else {
            // 没有内容，显示错误
            contentEl.innerHTML = `<div class="ai-search-error">获取烹饪说明失败，请稍后重试</div>`;
        }
    };

    eventSource.onopen = function() {
        console.log('AI 搜索 SSE 连接已建立');
    };
}

/**
 * 关闭 AI 搜索弹窗
 */
function closeAiSearchModal(event, forceClose) {
    if (forceClose || event.target.id === 'aiSearchModal') {
        // 关闭 SSE 连接
        if (currentAiSearchEventSource) {
            currentAiSearchEventSource.close();
            currentAiSearchEventSource = null;
        }
        document.getElementById('aiSearchModal').classList.remove('show');
    }
}

/**
 * 复制 AI 搜索内容
 */
async function copyAiSearchContent() {
    if (!currentAiSearchContent) {
        return;
    }

    try {
        await navigator.clipboard.writeText(currentAiSearchContent);
        const btn = document.getElementById('aiSearchCopyBtn');
        btn.textContent = '✅ 已复制';
        btn.classList.add('copied');
        setTimeout(() => {
            btn.textContent = '📋 复制';
            btn.classList.remove('copied');
        }, 2000);
    } catch (err) {
        console.error('复制失败:', err);
    }
}

/**
 * 简单的 Markdown 渲染（支持标题、列表、加粗、分隔线）
 */
function renderMarkdown(text) {
    if (!text) return '';

    return text
        // 转义 HTML
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        // 标题
        .replace(/^### (.+)$/gm, '<h3>$1</h3>')
        .replace(/^## (.+)$/gm, '<h2>$1</h2>')
        .replace(/^# (.+)$/gm, '<h1>$1</h1>')
        // 加粗
        .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
        // 分隔线
        .replace(/^---$/gm, '<hr>')
        // 无序列表
        .replace(/^- (.+)$/gm, '<li>$1</li>')
        .replace(/(<li>.*<\/li>\n?)+/g, '<ul>$&</ul>')
        // 有序列表
        .replace(/^\d+\. (.+)$/gm, '<li>$1</li>')
        // 段落
        .replace(/\n\n/g, '</p><p>')
        .replace(/\n/g, '<br>')
        // 包裹段落
        .replace(/^(.+)$/gm, function(match) {
            if (match.startsWith('<h') || match.startsWith('<ul') || match.startsWith('<ol') || match.startsWith('<hr') || match.startsWith('<li')) {
                return match;
            }
            return match;
        });
}
