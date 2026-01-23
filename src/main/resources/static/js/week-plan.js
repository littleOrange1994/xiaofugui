const WEEKLY_PLAN_API_BASE = '/api/weekly-plan';

document.addEventListener('DOMContentLoaded', function() {
    loadWeeklyPlan();
});

async function loadWeeklyPlan() {
    showLoading(true);
    try {
        const response = await fetch(WEEKLY_PLAN_API_BASE);
        const result = await response.json();
        if (result.code === 0) {
            renderWeeklyPlan(result.data || []);
        } else {
            renderError(result.message || '加载失败');
        }
    } catch (error) {
        console.error('加载本周计划失败:', error);
        renderError('网络错误，请稍后重试');
    } finally {
        showLoading(false);
    }
}

function renderWeeklyPlan(recipes) {
    const grid = document.getElementById('weeklyPlanGrid');
    const emptyState = document.getElementById('emptyState');

    if (!recipes || recipes.length === 0) {
        grid.innerHTML = '';
        emptyState.style.display = 'block';
        return;
    }

    emptyState.style.display = 'none';

    grid.innerHTML = recipes.map(recipe => `
        <div class="recipe-card" onclick="viewRecipe(${recipe.id})">
            <div class="card-image-wrapper">
                ${recipe.imageUrl
                    ? `<img src="${recipe.imageUrl}" alt="${escapeHtml(recipe.name)}" onerror="this.outerHTML='<div class=\\'placeholder-img\\'>🍲</div>'">`
                    : '<div class="placeholder-img">🍲</div>'
                }
                <div class="card-overlay">
                    <span class="overlay-text">👀 查看详情</span>
                </div>
            </div>
            <div class="recipe-card-content">
                <div class="weekly-card-title">
                    <h3>${escapeHtml(recipe.name)}</h3>
                    <button class="weekly-remove-btn" onclick="removeFromWeeklyPlan(${recipe.id}, event)">移除</button>
                </div>
                <div class="recipe-card-meta">
                    ${recipe.category ? `<span class="meta-tag cuisine">${escapeHtml(recipe.category)}</span>` : ''}
                    ${recipe.difficulty ? `<span class="meta-tag difficulty">${getDifficultyText(recipe.difficulty)}</span>` : ''}
                </div>
            </div>
        </div>
    `).join('');
}

function renderError(message) {
    const grid = document.getElementById('weeklyPlanGrid');
    grid.innerHTML = `
        <div class="empty-state" style="grid-column: 1/-1;">
            <div class="icon">😥</div>
            <p>${escapeHtml(message)}</p>
        </div>
    `;
}

function viewRecipe(id) {
    window.location.href = `detail.html?id=${id}`;
}

async function removeFromWeeklyPlan(recipeId, event) {
    if (event) {
        event.stopPropagation();
        event.preventDefault();
    }
    if (!recipeId) return;

    try {
        const response = await fetch(`${WEEKLY_PLAN_API_BASE}/remove`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ recipeId })
        });
        const result = await response.json();
        if (result.code === 0) {
            loadWeeklyPlan();
        } else {
            alert('移除失败：' + (result.message || '未知错误'));
        }
    } catch (error) {
        console.error('移除失败:', error);
        alert('网络错误，请稍后重试');
    }
}

function showLoading(show) {
    const loading = document.getElementById('loading');
    if (!loading) return;
    loading.style.display = show ? 'block' : 'none';
}

function getDifficultyText(level) {
    const texts = ['', '简单', '普通', '较难', '困难', '大师级'];
    return texts[level] || '未知';
}

function escapeHtml(text) {
    if (!text) return '';
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}
