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

// 页面加载时初始化
document.addEventListener('DOMContentLoaded', function() {
    initFilterTags();
    initSearchInput();
    loadRecipes();
});

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
        emptyState.style.display = 'block';
        return;
    }

    emptyState.style.display = 'none';

    grid.innerHTML = state.recipes.map(recipe => `
        <div class="recipe-card" onclick="viewRecipe(${recipe.id})">
            <div class="card-image-wrapper">
                ${recipe.imageUrl
                    ? `<img src="${recipe.imageUrl}" alt="${recipe.name}" onerror="this.outerHTML='<div class=\\'placeholder-img\\'>🍲</div>'">`
                    : '<div class="placeholder-img">🍲</div>'
                }
                <div class="card-overlay">
                    <span class="overlay-text">👀 查看详情</span>
                </div>
            </div>
            <div class="recipe-card-content">
                <h3>${escapeHtml(recipe.name)}</h3>
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
    window.location.href = `detail.html?id=${id}`;
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

// 获取当前季节
function getCurrentSeason() {
    const month = new Date().getMonth() + 1;
    if (month >= 3 && month <= 5) return { name: '春季', tip: '春季养生 · 清淡为主', categories: ['蒸菜', '汤类', '凉拌'] };
    if (month >= 6 && month <= 8) return { name: '夏季', tip: '夏季消暑 · 清凉解腻', categories: ['凉拌', '饮品', '汤类'] };
    if (month >= 9 && month <= 11) return { name: '秋季', tip: '秋季进补 · 润燥滋养', categories: ['炖菜', '汤类', '蒸菜'] };
    return { name: '冬季', tip: '冬季暖身 · 滋补养生', categories: ['炖菜', '砂锅菜', '煮锅', '汤类'] };
}

// 显示随机菜谱
async function showRandomRecipe() {
    const modal = document.getElementById('randomModal');
    const preview = document.getElementById('randomRecipePreview');
    const seasonTip = document.getElementById('seasonTip');
    const viewBtn = document.getElementById('viewRandomBtn');

    // 显示加载状态
    preview.innerHTML = '<div style="padding: 40px; color: #999;">🎲 正在为您挑选...</div>';
    modal.classList.add('show');

    // 设置季节提示
    const season = getCurrentSeason();
    seasonTip.textContent = `${season.name}推荐 · ${season.tip}`;

    try {
        // 随机选择一个适合季节的分类
        const category = season.categories[Math.floor(Math.random() * season.categories.length)];

        // 获取该分类的菜谱
        const response = await fetch(`${API_BASE}?page=1&pageSize=50&category=${encodeURIComponent(category)}`);
        const result = await response.json();

        if (result.code === 0 && result.data && result.data.records.length > 0) {
            const recipes = result.data.records;
            const randomRecipe = recipes[Math.floor(Math.random() * recipes.length)];

            // 渲染推荐菜品
            preview.innerHTML = `
                ${randomRecipe.imageUrl
                    ? `<img src="${randomRecipe.imageUrl}" alt="${randomRecipe.name}" onerror="this.src='data:image/svg+xml,<svg xmlns=%22http://www.w3.org/2000/svg%22 viewBox=%220 0 100 100%22><text y=%22.9em%22 font-size=%2280%22>🍲</text></svg>'">`
                    : '<div style="width:150px;height:150px;background:#fff5f5;border-radius:10px;display:flex;align-items:center;justify-content:center;font-size:4rem;margin:0 auto 15px;">🍲</div>'
                }
                <h3>${escapeHtml(randomRecipe.name)}</h3>
                <p style="color:#888;margin-top:10px;">
                    ${randomRecipe.category || ''} · ${getDifficultyText(randomRecipe.difficulty)} · ${getSpicyLevel(randomRecipe.spicyLevel)}
                </p>
            `;

            // 设置查看详情按钮
            viewBtn.onclick = () => {
                window.location.href = `detail.html?id=${randomRecipe.id}`;
            };
        } else {
            preview.innerHTML = '<div style="padding: 40px; color: #999;">暂无推荐，请稍后再试</div>';
        }
    } catch (error) {
        console.error('获取随机菜谱失败:', error);
        preview.innerHTML = '<div style="padding: 40px; color: #999;">获取推荐失败，请稍后再试</div>';
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
