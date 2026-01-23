const WEEKLY_PLAN_API_BASE = '/api/weekly-plan';
const PLAN_DAY_LABELS = {
    1: '周一',
    2: '周二',
    3: '周三',
    4: '周四',
    5: '周五',
    6: '周六',
    7: '周日'
};

// 存储当前正在进行的fetch请求
let pendingRequests = [];

document.addEventListener('DOMContentLoaded', function() {
    loadWeeklyPlan();
    // 优化性能：滚动时暂停飘浮动画
    optimizeFloatingAnimation();
});

async function loadWeeklyPlan() {
    showLoading(true);
    try {
        const response = await fetch(`${WEEKLY_PLAN_API_BASE}/items`);
        const result = await response.json();
        if (result.code === 0) {
            renderWeeklyPlan(result.data || []);
        } else {
            renderError(result.message || '加载失败');
        }
    } catch (error) {
        console.error('加载玉芳心愿单失败:', error);
        renderError('网络错误，请稍后重试');
    } finally {
        showLoading(false);
    }
}

function renderWeeklyPlan(items) {
    const container = document.getElementById('weeklyPlanContainer');
    const emptyState = document.getElementById('emptyState');

    // 更新统计数字
    updateStats(items);

    if (!items || items.length === 0) {
        container.innerHTML = '';
        emptyState.style.display = 'block';
        return;
    }

    emptyState.style.display = 'none';

    const grouped = groupItems(items);
    container.innerHTML = renderGroupedSections(grouped);
}

// 更新统计数据
function updateStats(items) {
    const total = items ? items.length : 0;
    let assigned = 0;
    let unassigned = 0;

    if (items) {
        items.forEach(item => {
            const planDay = Number(item.planDay || 0);
            if (planDay >= 1 && planDay <= 7) {
                assigned++;
            } else {
                unassigned++;
            }
        });
    }

    const totalEl = document.getElementById('totalCount');
    const assignedEl = document.getElementById('assignedCount');
    const unassignedEl = document.getElementById('unassignedCount');

    if (totalEl) totalEl.textContent = total;
    if (assignedEl) assignedEl.textContent = assigned;
    if (unassignedEl) unassignedEl.textContent = unassigned;
}

function renderError(message) {
    const container = document.getElementById('weeklyPlanContainer');
    container.innerHTML = `
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

async function clearWeeklyPlan() {
    const ok = window.confirm('确定清空本周玉芳心愿单吗？');
    if (!ok) return;

    try {
        const response = await fetch(`${WEEKLY_PLAN_API_BASE}/clear`, { method: 'POST' });
        const result = await response.json();
        if (result.code === 0) {
            loadWeeklyPlan();
        } else {
            alert('清空失败：' + (result.message || '未知错误'));
        }
    } catch (error) {
        console.error('清空失败:', error);
        alert('网络错误，请稍后重试');
    }
}

async function assignPlanDay(recipeId, value, event) {
    if (event) {
        event.stopPropagation();
        event.preventDefault();
    }
    const planDay = value ? Number(value) : 0;

    try {
        const response = await fetch(`${WEEKLY_PLAN_API_BASE}/assign`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ recipeId, planDay })
        });
        const result = await response.json();
        if (result.code === 0) {
            loadWeeklyPlan();
        } else {
            alert('分配失败：' + (result.message || '未知错误'));
        }
    } catch (error) {
        console.error('分配失败:', error);
        alert('网络错误，请稍后重试');
    }
}

async function saveRemark(recipeId, event) {
    if (event) {
        event.stopPropagation();
        event.preventDefault();
    }
    const input = document.getElementById(`remark-input-${recipeId}`);
    if (!input) return;

    const remark = input.value || '';
    if (remark.trim().length > 200) {
        alert('备注最多200字');
        return;
    }

    const btn = document.getElementById(`remark-save-${recipeId}`);
    if (btn) btn.disabled = true;

    try {
        const response = await fetch(`${WEEKLY_PLAN_API_BASE}/remark`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ recipeId, remark })
        });
        const result = await response.json();
        if (result.code === 0) {
            if (btn) {
                const old = btn.textContent;
                btn.textContent = '已保存';
                setTimeout(() => {
                    btn.textContent = old;
                }, 800);
            }
        } else {
            alert('保存失败：' + (result.message || '未知错误'));
        }
    } catch (error) {
        console.error('保存备注失败:', error);
        alert('网络错误，请稍后重试');
    } finally {
        if (btn) btn.disabled = false;
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

function escapeAttr(text) {
    return escapeHtml(text).replace(/[\r\n]+/g, ' ');
}

function stopCardClick(event) {
    if (!event) return;
    event.stopPropagation();
    event.preventDefault();
}

function groupItems(items) {
    const grouped = { unassigned: [] };
    for (let i = 1; i <= 7; i++) {
        grouped[i] = [];
    }

    items.forEach(item => {
        const recipe = item.recipe;
        if (!recipe) return;

        const planDay = Number(item.planDay || 0);
        const targetKey = planDay >= 1 && planDay <= 7 ? planDay : 'unassigned';
        grouped[targetKey].push(item);
    });
    return grouped;
}

function renderGroupedSections(grouped) {
    const order = ['unassigned', 1, 2, 3, 4, 5, 6, 7];
    return order.map(key => {
        const items = grouped[key] || [];
        if (items.length === 0) {
            return '';
        }

        const title = key === 'unassigned' ? '未分配' : PLAN_DAY_LABELS[key];
        return `
            <div class="weekly-plan-section">
                <div class="weekly-plan-section-header">
                    <div class="weekly-plan-section-title">${title}</div>
                    <div class="weekly-plan-section-count">${items.length} 道</div>
                </div>
                <div class="recipe-grid">
                    ${items.map(renderItemCard).join('')}
                </div>
            </div>
        `;
    }).join('');
}

function renderItemCard(item) {
    const recipe = item.recipe;
    const planDay = Number(item.planDay || 0);
    const remark = item.remark || '';
    const selectHtml = renderPlanDaySelect(recipe.id, planDay);
    const remarkValue = escapeAttr(remark);

    return `
        <div class="recipe-card" onclick="viewRecipe(${recipe.id})">
            <div class="card-image-wrapper">
                ${recipe.imageUrl
                    ? `<img src="${recipe.imageUrl}" alt="${escapeHtml(recipe.name)}" loading="lazy" onerror="this.outerHTML='<div class=\\'placeholder-img\\'>🍲</div>'">`
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
                <div class="weekly-plan-controls" onclick="stopCardClick(event)">
                    <span class="weekly-plan-label">安排：</span>
                    ${selectHtml}
                </div>
                <div class="weekly-plan-remark" onclick="stopCardClick(event)">
                    <input id="remark-input-${recipe.id}" class="weekly-remark-input" maxlength="200"
                           placeholder="备注（最多200字）" value="${remarkValue}">
                    <button id="remark-save-${recipe.id}" class="weekly-remark-save"
                            onclick="saveRemark(${recipe.id}, event)">保存</button>
                </div>
            </div>
        </div>
    `;
}

function renderPlanDaySelect(recipeId, planDay) {
    const options = [
        { value: '', text: '未分配' },
        { value: 1, text: '周一' },
        { value: 2, text: '周二' },
        { value: 3, text: '周三' },
        { value: 4, text: '周四' },
        { value: 5, text: '周五' },
        { value: 6, text: '周六' },
        { value: 7, text: '周日' }
    ];

    const optionHtml = options.map(opt => {
        const selected = String(opt.value) === String(planDay) ? 'selected' : '';
        return `<option value="${opt.value}" ${selected}>${opt.text}</option>`;
    }).join('');

    return `
        <select class="weekly-plan-select" onchange="assignPlanDay(${recipeId}, this.value, event)">
            ${optionHtml}
        </select>
    `;
}

/**
 * 优化的返回首页函数
 * 清理资源，取消请求，快速跳转
 */
function goBackHome() {
    // 1. 取消所有未完成的fetch请求
    pendingRequests.forEach(controller => {
        try {
            controller.abort();
        } catch (e) {
            // 忽略已经完成的请求
        }
    });
    pendingRequests = [];

    // 2. 停止所有CSS动画
    const floatingBg = document.querySelector('.floating-food-bg');
    if (floatingBg) {
        floatingBg.style.display = 'none';
    }

    // 3. 清空页面内容减少内存占用
    const container = document.getElementById('weeklyPlanContainer');
    if (container) {
        container.innerHTML = '';
    }

    // 4. 使用replace避免在历史记录中留痕，提升后退性能
    window.location.replace('index.html');
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
