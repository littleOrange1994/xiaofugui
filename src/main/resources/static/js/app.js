// 全局状态
const state = {
    recipes: [],
    currentPage: 1,
    pageSize: 12,
    total: 0,
    cuisineType: '',
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
            // 添加当前 active 状态
            this.classList.add('active');
            // 更新筛选条件
            state.cuisineType = this.dataset.cuisine;
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
    state.cuisineType = '';
    state.currentPage = 1;

    // 重置筛选标签状态
    const tags = document.querySelectorAll('.filter-tag');
    tags.forEach(t => t.classList.remove('active'));
    document.querySelector('.filter-tag[data-cuisine=""]').classList.add('active');

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
        } else {
            // 列表模式
            url = `${API_BASE}?page=${state.currentPage}&pageSize=${state.pageSize}`;
            if (state.cuisineType) {
                url += `&cuisineType=${encodeURIComponent(state.cuisineType)}`;
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
            ${recipe.imageUrl
                ? `<img src="${recipe.imageUrl}" alt="${recipe.name}" onerror="this.outerHTML='<div class=\\'placeholder-img\\'>🍲</div>'">`
                : '<div class="placeholder-img">🍲</div>'
            }
            <div class="recipe-card-content">
                <h3>${escapeHtml(recipe.name)}</h3>
                <div class="recipe-card-meta">
                    ${recipe.cuisineType ? `<span class="meta-tag cuisine">${escapeHtml(recipe.cuisineType)}</span>` : ''}
                    ${recipe.cookingTime ? `<span class="meta-tag time">${recipe.cookingTime}分钟</span>` : ''}
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
    if (!level || level === 0) return '不辣';
    return '🌶️'.repeat(Math.min(level, 5));
}

// 获取推荐星级
function getRecommendScore(score) {
    if (!score) return '';
    const fullStars = Math.floor(score / 2);
    const halfStar = score % 2 >= 1;
    let stars = '⭐'.repeat(fullStars);
    if (halfStar) stars += '✨';
    return stars;
}

// HTML 转义
function escapeHtml(text) {
    if (!text) return '';
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}
