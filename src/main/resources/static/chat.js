// 全局变量
let conversationMessages = [];
let selectedImages = [];
let userId = generateUserId();
let sessionId = generateSessionId();

// 初始化
document.addEventListener('DOMContentLoaded', function() {
    initializeChat();
    setupEventListeners();
});

// 初始化聊天
function initializeChat() {
    // 自动调整输入框高度
    const messageInput = document.getElementById('messageInput');
    messageInput.addEventListener('input', autoResizeTextarea);

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
    const hasText = messageInput.value.trim().length > 0;
    const hasImages = selectedImages.length > 0;

    sendBtn.disabled = !(hasText || hasImages);
}

// 发送消息
async function sendMessage() {
    const messageInput = document.getElementById('messageInput');
    const messageText = messageInput.value.trim();

    if (!messageText && selectedImages.length === 0) {
        return;
    }

    // 构建消息对象 - 使用 ContentPart 格式
    const contentParts = [];

    // 添加文本内容
    if (messageText) {
        contentParts.push({
            type: 'text',
            text: messageText
        });
    }

    // 添加图片内容
    if (selectedImages.length > 0) {
        selectedImages.forEach(imageData => {
            contentParts.push({
                type: 'image_url',
                image_url: {
                    url: imageData
                }
            });
        });
    }

    const userMessage = {
        role: 'user',
        content: contentParts,
        timestamp: Date.now()
    };

    // 添加到对话历史
    conversationMessages.push(userMessage);

    // 显示用户消息
    displayUserMessage(messageText, selectedImages);

    // 清空输入
    messageInput.value = '';
    autoResizeTextarea();

    // 清空图片
    const imagesToSend = [...selectedImages];
    clearSelectedImages();

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

        // 如果有图片，添加到请求中
        if (imagesToSend.length > 0) {
            requestBody.images = imagesToSend;
        }

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
function displayUserMessage(text, images) {
    const messagesContainer = document.getElementById('chatMessages');
    const messageWrapper = document.createElement('div');
    messageWrapper.className = 'message-wrapper user-message';

    let imagesHtml = '';
    if (images && images.length > 0) {
        const imagesList = images.map(img =>
            `<img src="${img}" alt="用户上传的图片" class="message-image" onclick="previewImage('${img}')">`
        ).join('');

        imagesHtml = `<div class="message-images">${imagesList}</div>`;
    }

    messageWrapper.innerHTML = `
        <div class="message-avatar user-avatar">我</div>
        <div class="message-content">
            <div class="message-bubble">
                ${text ? `<p>${escapeHtml(text)}</p>` : ''}
                ${imagesHtml}
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
    let userId = localStorage.getItem('shengong_user_id');
    if (!userId) {
        userId = 'user_' + Date.now() + '_' + Math.random().toString(36).substring(2, 11);
        localStorage.setItem('shengong_user_id', userId);
    }
    return userId;
}

// 生成会话ID
function generateSessionId() {
    return 'session_' + Date.now() + '_' + Math.random().toString(36).substring(2, 11);
}

// 切换图片上传
function toggleImageUpload() {
    document.getElementById('imageInput').click();
}

// 处理图片选择
function handleImageSelect(event) {
    const files = event.target.files;

    if (files.length === 0) return;

    // 限制图片数量
    const maxImages = 4;
    if (selectedImages.length + files.length > maxImages) {
        alert(`最多只能上传${maxImages}张图片`);
        return;
    }

    // 处理每个文件
    Array.from(files).forEach(file => {
        // 检查文件类型
        if (!file.type.startsWith('image/')) {
            alert('请选择图片文件');
            return;
        }

        // 检查文件大小 (限制5MB)
        if (file.size > 5 * 1024 * 1024) {
            alert('图片大小不能超过5MB');
            return;
        }

        // 读取文件并转换为Base64
        const reader = new FileReader();
        reader.onload = function(e) {
            const base64Data = e.target.result;
            selectedImages.push(base64Data);
            displayImagePreview(base64Data);
            updateSendButtonState();
        };
        reader.readAsDataURL(file);
    });

    // 清空input，允许重复选择相同文件
    event.target.value = '';
}

// 显示图片预览
function displayImagePreview(imageData) {
    const container = document.getElementById('imagePreviewContainer');
    const list = document.getElementById('imagePreviewList');

    const previewItem = document.createElement('div');
    previewItem.className = 'image-preview-item';
    previewItem.dataset.image = imageData;

    previewItem.innerHTML = `
        <img src="${imageData}" alt="预览图片">
        <button class="image-preview-remove" onclick="removeImagePreview(this)">×</button>
    `;

    list.appendChild(previewItem);
    container.style.display = 'block';
}

// 移除图片预览
function removeImagePreview(button) {
    const previewItem = button.parentElement;
    const imageData = previewItem.dataset.image;

    // 从数组中移除
    const index = selectedImages.indexOf(imageData);
    if (index > -1) {
        selectedImages.splice(index, 1);
    }

    // 移除DOM元素
    previewItem.remove();

    // 如果没有图片了，隐藏预览容器
    if (selectedImages.length === 0) {
        document.getElementById('imagePreviewContainer').style.display = 'none';
    }

    updateSendButtonState();
}

// 清空选中的图片
function clearSelectedImages() {
    selectedImages = [];
    document.getElementById('imagePreviewList').innerHTML = '';
    document.getElementById('imagePreviewContainer').style.display = 'none';
    updateSendButtonState();
}

// 预览图片(全屏)
function previewImage(src) {
    // 简单实现：在新窗口打开
    window.open(src, '_blank');
}

// 切换菜单
function toggleMenu() {
    const overlay = document.getElementById('menuOverlay');
    const panel = document.getElementById('menuPanel');

    overlay.classList.toggle('active');
    panel.classList.toggle('active');
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
