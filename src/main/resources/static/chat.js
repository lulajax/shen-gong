// 本地存储键名
const STORAGE_KEYS = {
    MESSAGES: 'shengong_chat_messages',
    SESSION_ID: 'shengong_session_id',
    USER_ID: 'shengong_user_id',
    SESSIONS_LIST: 'shengong_sessions_list'  // 会话列表
};

// 全局变量
let conversationMessages = [];
let userId = generateUserId();
let sessionId = loadOrCreateSessionId();

// 初始化
document.addEventListener('DOMContentLoaded', function() {
    initializeChat();
    setupEventListeners();
});

// 初始化聊天
function initializeChat() {
    // 加载历史消息
    loadChatHistory();

    // 自动调整输入框高度
    const messageInput = document.getElementById('messageInput');
    messageInput.addEventListener('input', autoResizeTextarea);

    // 初始化发送按钮状态
    updateSendButtonState();

    // 滚动到底部
    scrollToBottom();
}

// 设置事件监听器
function setupEventListeners() {
    const messageInput = document.getElementById('messageInput');
    const sendBtn = document.getElementById('sendBtn');

    // 监听输入变化，控制发送按钮状态
    messageInput.addEventListener('input', function() {
        updateSendButtonState();
    });

    // 监听回车键发送消息
    messageInput.addEventListener('keydown', function(e) {
        if (e.key === 'Enter' && !e.shiftKey) {
            e.preventDefault();
            if (!sendBtn.disabled) {
                sendMessage();
            }
        }
    });
}

// 自动调整输入框高度
function autoResizeTextarea() {
    const textarea = document.getElementById('messageInput');
    textarea.style.height = 'auto';
    textarea.style.height = Math.min(textarea.scrollHeight, 120) + 'px';
}

// 更新发送按钮状态
function updateSendButtonState() {
    const messageInput = document.getElementById('messageInput');
    const sendBtn = document.getElementById('sendBtn');

    if (!messageInput || !sendBtn) {
        return;
    }

    const hasText = messageInput.value.trim().length > 0;
    sendBtn.disabled = !hasText;
}

// 发送消息
async function sendMessage() {
    const messageInput = document.getElementById('messageInput');
    const messageText = messageInput.value.trim();

    if (!messageText) {
        return;
    }

    // 构建消息对象 - 使用 ContentPart 格式
    const contentParts = [{
        type: 'text',
        text: messageText
    }];

    const userMessage = {
        role: 'user',
        content: contentParts,
        timestamp: Date.now()
    };

    // 添加到对话历史
    conversationMessages.push(userMessage);

    // 保存到本地存储
    saveChatHistory();

    // 显示用户消息
    displayUserMessage(messageText);

    // 清空输入
    messageInput.value = '';
    autoResizeTextarea();

    // 滚动到底部
    scrollToBottom();

    // 显示加载指示器
    showLoading();

    try {
        // 构建请求体
        const requestBody = {
            userId: userId,
            sessionId: sessionId,
            messages: conversationMessages
        };

        // 调用后端API
        const response = await fetch('/api/v1/chat/send', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(requestBody)
        });

        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }

        const data = await response.json();

        // 隐藏加载指示器
        hideLoading();

        // 处理响应
        if (data.success) {
            const responseText = data.data?.result || data.message || '抱歉，我没有理解您的意思。';

            const botMessage = {
                role: 'assistant',
                content: [{
                    type: 'text',
                    text: responseText
                }],
                timestamp: Date.now()
            };

            // 添加到对话历史
            conversationMessages.push(botMessage);

            // 保存到本地存储
            saveChatHistory();

            // 显示助手消息
            displayBotMessage(responseText);
        } else {
            displayErrorMessage(data.message || '抱歉，处理您的请求时出现了问题。');
        }

    } catch (error) {
        console.error('发送消息失败:', error);
        hideLoading();
        displayErrorMessage('抱歉，网络连接出现问题，请稍后重试。');
    }

    // 滚动到底部
    scrollToBottom();
}

// 显示用户消息
function displayUserMessage(text) {
    const messagesContainer = document.getElementById('chatMessages');
    const messageWrapper = document.createElement('div');
    messageWrapper.className = 'message-wrapper user-message';

    messageWrapper.innerHTML = `
        <div class="message-avatar user-avatar">我</div>
        <div class="message-content">
            <div class="message-bubble">
                <p>${escapeHtml(text)}</p>
            </div>
            <div class="message-time">${getCurrentTime()}</div>
        </div>
    `;

    messagesContainer.appendChild(messageWrapper);
}

// 显示助手消息
function displayBotMessage(text) {
    const messagesContainer = document.getElementById('chatMessages');
    const messageWrapper = document.createElement('div');
    messageWrapper.className = 'message-wrapper bot-message';

    messageWrapper.innerHTML = `
        <div class="message-avatar bot-avatar">AI</div>
        <div class="message-content">
            <div class="message-bubble">
                <p>${formatBotMessage(text)}</p>
            </div>
            <div class="message-time">${getCurrentTime()}</div>
        </div>
    `;

    messagesContainer.appendChild(messageWrapper);
}

// 显示错误消息
function displayErrorMessage(text) {
    const messagesContainer = document.getElementById('chatMessages');
    const messageWrapper = document.createElement('div');
    messageWrapper.className = 'message-wrapper bot-message';

    messageWrapper.innerHTML = `
        <div class="message-avatar bot-avatar">AI</div>
        <div class="message-content">
            <div class="message-bubble" style="background: #fff2e8; border: 1px solid #ffbb96;">
                <p style="color: #d4380d;">${escapeHtml(text)}</p>
            </div>
            <div class="message-time">${getCurrentTime()}</div>
        </div>
    `;

    messagesContainer.appendChild(messageWrapper);
}

// 格式化助手消息(支持简单的Markdown)
function formatBotMessage(text) {
    if (!text) return '';

    // 转义HTML
    let formatted = escapeHtml(text);

    // 支持换行
    formatted = formatted.replace(/\n/g, '<br>');

    // 支持加粗 **text**
    formatted = formatted.replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>');

    // 支持斜体 *text*
    formatted = formatted.replace(/\*(.+?)\*/g, '<em>$1</em>');

    // 支持代码 `code`
    formatted = formatted.replace(/`(.+?)`/g, '<code style="background: #f5f5f5; padding: 2px 6px; border-radius: 3px; font-family: monospace;">$1</code>');

    return formatted;
}

// HTML转义
function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

// 滚动到底部
function scrollToBottom() {
    const messagesContainer = document.getElementById('chatMessages');
    setTimeout(() => {
        messagesContainer.scrollTop = messagesContainer.scrollHeight;
    }, 100);
}

// 显示加载指示器
function showLoading() {
    document.getElementById('loadingIndicator').style.display = 'block';
}

// 隐藏加载指示器
function hideLoading() {
    document.getElementById('loadingIndicator').style.display = 'none';
}

// 获取当前时间
function getCurrentTime() {
    const now = new Date();
    return now.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' });
}

// 生成用户ID
function generateUserId() {
    let userId = localStorage.getItem(STORAGE_KEYS.USER_ID);
    if (!userId) {
        userId = 'user_' + Date.now() + '_' + Math.random().toString(36).substring(2, 11);
        localStorage.setItem(STORAGE_KEYS.USER_ID, userId);
    }
    return userId;
}

// 加载或创建会话ID
function loadOrCreateSessionId() {
    let sessionId = localStorage.getItem(STORAGE_KEYS.SESSION_ID);
    if (!sessionId) {
        sessionId = generateSessionId();
        localStorage.setItem(STORAGE_KEYS.SESSION_ID, sessionId);
    }
    return sessionId;
}

// 生成会话ID
function generateSessionId() {
    return 'session_' + Date.now() + '_' + Math.random().toString(36).substring(2, 11);
}

// 保存聊天历史到本地存储
function saveChatHistory() {
    try {
        // 保存当前会话
        const chatData = {
            messages: conversationMessages,
            sessionId: sessionId,
            lastUpdate: Date.now()
        };

        // 保存到通用位置（用于页面刷新时加载）
        localStorage.setItem(STORAGE_KEYS.MESSAGES, JSON.stringify(chatData));

        // 同时保存到会话专用位置
        const sessionKey = `shengong_session_${sessionId}`;
        localStorage.setItem(sessionKey, JSON.stringify(chatData));

        // 更新会话列表
        updateSessionsList();
    } catch (error) {
        console.error('保存聊天历史失败:', error);
        // 如果存储空间满了，清除旧数据
        if (error.name === 'QuotaExceededError') {
            console.warn('本地存储空间已满，清除旧数据');
            clearOldChatHistory();
        }
    }
}

// 加载聊天历史
function loadChatHistory() {
    try {
        const savedData = localStorage.getItem(STORAGE_KEYS.MESSAGES);
        if (savedData) {
            const chatData = JSON.parse(savedData);

            // 检查是否是同一个会话
            if (chatData.sessionId === sessionId) {
                conversationMessages = chatData.messages || [];

                // 重新渲染所有历史消息
                renderChatHistory();
            } else {
                // 会话ID不匹配，开始新会话
            }
        }
    } catch (error) {
        console.error('加载聊天历史失败:', error);
    }
}

// 渲染聊天历史
function renderChatHistory() {
    const messagesContainer = document.getElementById('chatMessages');
    // 清空现有消息（保留欢迎消息除外）
    messagesContainer.innerHTML = '';

    if (conversationMessages.length === 0) {
        // 显示欢迎消息
        messagesContainer.innerHTML = `
            <div class="message-wrapper bot-message">
                <div class="message-avatar bot-avatar">AI</div>
                <div class="message-content">
                    <div class="message-bubble">
                        <p>你好！我是智能助手，很高兴为您服务。有什么我可以帮助您的吗？</p>
                    </div>
                    <div class="message-time">刚刚</div>
                </div>
            </div>
        `;
        return;
    }

    // 渲染所有历史消息
    conversationMessages.forEach(msg => {
        // 提取文本内容
        let textContent = '';

        if (msg.content && Array.isArray(msg.content)) {
            msg.content.forEach(part => {
                if (part.type === 'text' && part.text) {
                    textContent += part.text;
                }
            });
        }

        // 根据角色显示消息
        if (msg.role === 'user') {
            displayUserMessage(textContent);
        } else if (msg.role === 'assistant') {
            displayBotMessage(textContent);
        }
    });
}

// 清除旧的聊天历史
function clearOldChatHistory() {
    localStorage.removeItem(STORAGE_KEYS.MESSAGES);
    conversationMessages = [];
}

// 更新会话列表
function updateSessionsList() {
    try {
        if (conversationMessages.length === 0) {
            return; // 空会话不保存
        }

        const sessionsList = getSessionsList();

        // 生成会话摘要（取第一条用户消息的前30个字符）
        let summary = '新对话';
        const firstUserMsg = conversationMessages.find(msg => msg.role === 'user');
        if (firstUserMsg && firstUserMsg.content) {
            const textContent = firstUserMsg.content
                .filter(part => part.type === 'text')
                .map(part => part.text)
                .join(' ');
            summary = textContent.substring(0, 30) + (textContent.length > 30 ? '...' : '');
        }

        // 查找现有会话索引
        const existingIndex = sessionsList.findIndex(s => s.sessionId === sessionId);

        const sessionInfo = {
            sessionId: sessionId,
            summary: summary,
            lastUpdate: Date.now(),
            messageCount: conversationMessages.length
        };

        if (existingIndex >= 0) {
            // 更新现有会话
            sessionsList[existingIndex] = sessionInfo;
        } else {
            // 添加新会话
            sessionsList.unshift(sessionInfo);
        }

        // 只保留最近20个会话
        if (sessionsList.length > 20) {
            sessionsList.splice(20);
        }

        localStorage.setItem(STORAGE_KEYS.SESSIONS_LIST, JSON.stringify(sessionsList));
    } catch (error) {
        console.error('更新会话列表失败:', error);
    }
}

// 获取会话列表
function getSessionsList() {
    try {
        const data = localStorage.getItem(STORAGE_KEYS.SESSIONS_LIST);
        return data ? JSON.parse(data) : [];
    } catch (error) {
        console.error('读取会话列表失败:', error);
        return [];
    }
}

// 加载指定会话
function loadSession(targetSessionId) {
    try {
        // 保存当前会话
        if (conversationMessages.length > 0) {
            saveChatHistory();
        }

        // 从localStorage加载指定会话的数据
        const storageKey = `shengong_session_${targetSessionId}`;
        const savedData = localStorage.getItem(storageKey);

        if (savedData) {
            const chatData = JSON.parse(savedData);
            conversationMessages = chatData.messages || [];
            sessionId = targetSessionId;

            // 更新当前会话ID
            localStorage.setItem(STORAGE_KEYS.SESSION_ID, sessionId);
            localStorage.setItem(STORAGE_KEYS.MESSAGES, savedData);

            // 重新渲染
            renderChatHistory();
            scrollToBottom();

            // 关闭菜单
            toggleMenu();
        } else {
            alert('会话数据不存在');
        }
    } catch (error) {
        console.error('加载会话失败:', error);
        alert('加载会话失败');
    }
}

// 删除指定会话
function deleteSession(targetSessionId, event) {
    if (event) {
        event.stopPropagation();
    }

    if (!confirm('确定要删除这个会话吗？')) {
        return;
    }

    try {
        // 从会话列表中删除
        let sessionsList = getSessionsList();
        sessionsList = sessionsList.filter(s => s.sessionId !== targetSessionId);
        localStorage.setItem(STORAGE_KEYS.SESSIONS_LIST, JSON.stringify(sessionsList));

        // 删除会话数据
        const storageKey = `shengong_session_${targetSessionId}`;
        localStorage.removeItem(storageKey);

        // 如果删除的是当前会话，创建新会话
        if (targetSessionId === sessionId) {
            startNewChat();
        } else {
            // 刷新会话列表显示
            renderSessionsList();
        }
    } catch (error) {
        console.error('删除会话失败:', error);
        alert('删除会话失败');
    }
}

// 渲染会话列表到菜单
function renderSessionsList() {
    const sessionsList = getSessionsList();
    const container = document.getElementById('sessionsListContainer');

    if (!container) return;

    if (sessionsList.length === 0) {
        container.innerHTML = '<div class="sessions-empty">暂无历史会话</div>';
        return;
    }

    let html = '';
    sessionsList.forEach(session => {
        const isActive = session.sessionId === sessionId;
        const date = new Date(session.lastUpdate).toLocaleString('zh-CN', {
            month: '2-digit',
            day: '2-digit',
            hour: '2-digit',
            minute: '2-digit'
        });

        html += `
            <div class="session-item ${isActive ? 'active' : ''}" onclick="loadSession('${session.sessionId}')">
                <div class="session-info">
                    <div class="session-summary">${escapeHtml(session.summary)}</div>
                    <div class="session-meta">
                        <span>${date}</span>
                        <span>${session.messageCount}条消息</span>
                    </div>
                </div>
                <button class="session-delete-btn" onclick="deleteSession('${session.sessionId}', event)" title="删除">
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <polyline points="3 6 5 6 21 6"/>
                        <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/>
                    </svg>
                </button>
            </div>
        `;
    });

    container.innerHTML = html;
}

// 切换菜单
function toggleMenu() {
    const overlay = document.getElementById('menuOverlay');
    const panel = document.getElementById('menuPanel');

    const isOpening = !panel.classList.contains('active');

    overlay.classList.toggle('active');
    panel.classList.toggle('active');

    // 打开菜单时渲染会话列表
    if (isOpening) {
        renderSessionsList();
    }
}

// 新建对话
function startNewChat() {
    if (conversationMessages.length > 0) {
        if (!confirm('确定要开始新对话吗？当前对话将被保存。')) {
            return;
        }
    }

    // 保存当前对话（如果有的话）
    if (conversationMessages.length > 0) {
        saveChatHistory();
    }

    // 清空当前对话
    conversationMessages = [];

    // 生成新的会话ID
    sessionId = generateSessionId();
    localStorage.setItem(STORAGE_KEYS.SESSION_ID, sessionId);

    // 清除当前会话的存储
    localStorage.removeItem(STORAGE_KEYS.MESSAGES);

    // 重新渲染界面
    renderChatHistory();

    // 滚动到顶部
    scrollToBottom();

    toggleMenu();
}

// 清空对话
function clearChat() {
    if (!confirm('确定要清空所有对话吗?')) {
        return;
    }

    conversationMessages = [];
    document.getElementById('chatMessages').innerHTML = `
        <div class="message-wrapper bot-message">
            <div class="message-avatar bot-avatar">AI</div>
            <div class="message-content">
                <div class="message-bubble">
                    <p>对话已清空。有什么我可以帮助您的吗？</p>
                </div>
                <div class="message-time">${getCurrentTime()}</div>
            </div>
        </div>
    `;

    // 生成新的会话ID
    sessionId = generateSessionId();
    localStorage.setItem(STORAGE_KEYS.SESSION_ID, sessionId);

    // 清除本地存储的聊天历史
    localStorage.removeItem(STORAGE_KEYS.MESSAGES);

    toggleMenu();
}

// 导出对话
function exportChat() {
    if (conversationMessages.length === 0) {
        alert('暂无对话内容可导出');
        return;
    }

    // 构建导出文本
    let exportText = 'Agent智能助手对话记录\n';
    exportText += '导出时间: ' + new Date().toLocaleString('zh-CN') + '\n';
    exportText += '=' .repeat(50) + '\n\n';

    conversationMessages.forEach((msg) => {
        const role = msg.role === 'user' ? '用户' : 'AI助手';
        // 提取文本内容
        let textContent = '';
        if (msg.content && Array.isArray(msg.content)) {
            msg.content.forEach(part => {
                if (part.type === 'text' && part.text) {
                    textContent += part.text;
                }
            });
        }
        exportText += `[${role}]: ${textContent}\n\n`;
    });

    // 下载文件
    const blob = new Blob([exportText], { type: 'text/plain;charset=utf-8' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `神工对话记录_${new Date().getTime()}.txt`;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);

    toggleMenu();
}

// 显示设置
function showSettings() {
    alert('设置功能开发中...');
    toggleMenu();
}
