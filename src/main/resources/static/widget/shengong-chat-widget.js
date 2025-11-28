/**
 * 神工聊天组件 Widget SDK
 * 提供跨域嵌入式聊天浮窗功能
 *
 * @version 1.0.0
 * @author 神工团队
 */
(function(window, document) {
  'use strict';

  /**
   * 神工聊天组件 Widget 类
   */
  class ShengongChatWidget {
    constructor() {
      this.config = null;
      this.container = null;
      this.button = null;
      this.window = null;
      this.iframe = null;
      this.isOpen = false;
      this.isInitialized = false;
    }

    /**
     * 初始化 Widget
     * @param {Object} options 配置选项
     * @param {string} options.userId 用户ID（必填）
     * @param {string} options.apiBase API基础URL（必填）
     * @param {string} options.position 浮窗位置，默认 'bottom-right'
     * @param {Object} options.theme 主题配置
     */
    init(options) {
      if (this.isInitialized) {
        console.warn('[ShengongChat] Widget已初始化，请勿重复初始化');
        return this;
      }

      // 验证必填参数
      if (!options || !options.userId) {
        throw new Error('[ShengongChat] userId 是必填参数');
      }
      if (!options.apiBase) {
        throw new Error('[ShengongChat] apiBase 是必填参数');
      }

      // 保存配置
      this.config = {
        userId: options.userId,
        apiBase: options.apiBase.replace(/\/$/, ''), // 移除末尾斜杠
        position: options.position || 'bottom-right',
        theme: options.theme || {}
      };

      // 注入样式
      this.injectStyles();

      // 创建 Widget
      this.createWidget();

      // 设置事件监听
      this.setupEvents();

      this.isInitialized = true;

      console.log('[ShengongChat] Widget 初始化成功:', this.config);
      return this;
    }

    /**
     * 动态注入样式
     */
    injectStyles() {
      if (document.getElementById('shengong-chat-widget-styles')) {
        return; // 已注入
      }

      const style = document.createElement('style');
      style.id = 'shengong-chat-widget-styles';
      style.textContent = `
        /* 神工聊天组件样式 */
        .sg-widget-container {
          position: fixed;
          bottom: 20px;
          right: 20px;
          z-index: 999999;
          font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
        }

        .sg-chat-button {
          width: 60px;
          height: 60px;
          border-radius: 50%;
          background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
          border: none;
          box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
          cursor: pointer;
          display: flex;
          align-items: center;
          justify-content: center;
          transition: transform 0.2s, box-shadow 0.2s;
          position: relative;
        }

        .sg-chat-button:hover {
          transform: scale(1.05);
          box-shadow: 0 6px 16px rgba(0, 0, 0, 0.2);
        }

        .sg-chat-button:active {
          transform: scale(0.95);
        }

        .sg-chat-button svg {
          width: 28px;
          height: 28px;
          fill: white;
        }

        .sg-unread-badge {
          position: absolute;
          top: -4px;
          right: -4px;
          background: #ff4d4f;
          color: white;
          border-radius: 10px;
          padding: 2px 6px;
          font-size: 12px;
          font-weight: bold;
          min-width: 20px;
          text-align: center;
          display: none;
        }

        .sg-unread-badge.show {
          display: block;
        }

        .sg-chat-window {
          position: absolute;
          bottom: 80px;
          right: 0;
          width: 380px;
          height: 600px;
          background: white;
          border-radius: 12px;
          box-shadow: 0 12px 48px rgba(0, 0, 0, 0.2);
          opacity: 0;
          transform: scale(0.95) translateY(10px);
          transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
          pointer-events: none;
          overflow: hidden;
        }

        .sg-chat-window.open {
          opacity: 1;
          transform: scale(1) translateY(0);
          pointer-events: auto;
        }

        .sg-chat-window iframe {
          width: 100%;
          height: 100%;
          border: none;
          border-radius: 12px;
        }

        /* 移动端适配 */
        @media (max-width: 768px) {
          .sg-widget-container {
            bottom: 10px;
            right: 10px;
          }

          .sg-chat-button {
            width: 56px;
            height: 56px;
          }

          .sg-chat-button svg {
            width: 24px;
            height: 24px;
          }

          .sg-chat-window {
            position: fixed;
            top: 0;
            left: 0;
            right: 0;
            bottom: 0;
            width: 100vw;
            height: 100vh;
            border-radius: 0;
            transform: translateY(100%);
          }

          .sg-chat-window.open {
            transform: translateY(0);
          }
        }

        /* 关闭按钮（仅移动端显示） */
        .sg-close-btn {
          display: none;
          position: absolute;
          top: 10px;
          right: 10px;
          width: 32px;
          height: 32px;
          background: rgba(0, 0, 0, 0.5);
          border: none;
          border-radius: 50%;
          color: white;
          font-size: 20px;
          cursor: pointer;
          z-index: 10;
        }

        @media (max-width: 768px) {
          .sg-close-btn {
            display: flex;
            align-items: center;
            justify-content: center;
          }
        }
      `;
      document.head.appendChild(style);
    }

    /**
     * 创建 Widget DOM 结构
     */
    createWidget() {
      // 创建容器
      this.container = document.createElement('div');
      this.container.id = 'shengong-chat-widget';
      this.container.className = 'sg-widget-container';

      // 创建聊天按钮
      this.button = document.createElement('button');
      this.button.className = 'sg-chat-button';
      this.button.setAttribute('aria-label', '打开聊天窗口');
      this.button.innerHTML = `
        <svg viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
          <path d="M20 2H4c-1.1 0-2 .9-2 2v18l4-4h14c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2zm0 14H6l-2 2V4h16v12z"/>
          <path d="M7 9h2v2H7zm4 0h2v2h-2zm4 0h2v2h-2z"/>
        </svg>
        <span class="sg-unread-badge" id="sg-unread-badge"></span>
      `;

      // 创建聊天窗口
      this.window = document.createElement('div');
      this.window.className = 'sg-chat-window';

      // 创建 iframe
      this.iframe = document.createElement('iframe');
      this.iframe.src = `${this.config.apiBase}/embed/chat-embed.html`;
      this.iframe.setAttribute('allow', 'microphone; camera');
      this.iframe.setAttribute('title', '神工智能助手');

      // 创建关闭按钮（移动端）
      const closeBtn = document.createElement('button');
      closeBtn.className = 'sg-close-btn';
      closeBtn.innerHTML = '×';
      closeBtn.onclick = () => this.close();

      // 组装 DOM
      this.window.appendChild(closeBtn);
      this.window.appendChild(this.iframe);
      this.container.appendChild(this.button);
      this.container.appendChild(this.window);
      document.body.appendChild(this.container);
    }

    /**
     * 设置事件监听
     */
    setupEvents() {
      // 按钮点击事件
      this.button.addEventListener('click', () => {
        this.toggle();
      });

      // 监听来自 iframe 的消息
      window.addEventListener('message', (event) => {
        // 验证来源
        const iframeOrigin = new URL(this.iframe.src).origin;
        if (event.origin !== iframeOrigin) {
          return;
        }

        // 处理消息
        this.handleMessage(event.data);
      });

      // iframe 加载完成后初始化
      this.iframe.addEventListener('load', () => {
        console.log('[ShengongChat] iframe 加载完成，发送初始化消息');
        this.sendMessageToIframe({
          type: 'INIT',
          payload: {
            userId: this.config.userId,
            apiBase: this.config.apiBase
          }
        });
      });

      // ESC 键关闭
      document.addEventListener('keydown', (event) => {
        if (event.key === 'Escape' && this.isOpen) {
          this.close();
        }
      });
    }

    /**
     * 处理来自 iframe 的消息
     */
    handleMessage(data) {
      if (!data || !data.type) return;

      switch (data.type) {
        case 'CLOSE_WIDGET':
          this.close();
          break;

        case 'NEW_MESSAGE':
          this.showUnreadBadge(data.payload?.unreadCount || 0);
          break;

        case 'READY':
          console.log('[ShengongChat] iframe 就绪');
          break;

        default:
          console.log('[ShengongChat] 收到未知消息类型:', data.type);
      }
    }

    /**
     * 向 iframe 发送消息
     */
    sendMessageToIframe(message) {
      if (!this.iframe || !this.iframe.contentWindow) {
        console.warn('[ShengongChat] iframe 未就绪');
        return;
      }

      const targetOrigin = new URL(this.iframe.src).origin;
      this.iframe.contentWindow.postMessage(message, targetOrigin);
    }

    /**
     * 打开聊天窗口
     */
    open() {
      if (this.isOpen) return;

      this.window.classList.add('open');
      this.isOpen = true;
      this.hideUnreadBadge();

      console.log('[ShengongChat] 聊天窗口已打开');
    }

    /**
     * 关闭聊天窗口
     */
    close() {
      if (!this.isOpen) return;

      this.window.classList.remove('open');
      this.isOpen = false;

      console.log('[ShengongChat] 聊天窗口已关闭');
    }

    /**
     * 切换聊天窗口状态
     */
    toggle() {
      if (this.isOpen) {
        this.close();
      } else {
        this.open();
      }
    }

    /**
     * 显示未读消息角标
     */
    showUnreadBadge(count) {
      const badge = document.getElementById('sg-unread-badge');
      if (badge && count > 0) {
        badge.textContent = count > 99 ? '99+' : count;
        badge.classList.add('show');
      }
    }

    /**
     * 隐藏未读消息角标
     */
    hideUnreadBadge() {
      const badge = document.getElementById('sg-unread-badge');
      if (badge) {
        badge.classList.remove('show');
      }
    }

    /**
     * 销毁 Widget
     */
    destroy() {
      if (!this.isInitialized) return;

      // 移除 DOM
      if (this.container && this.container.parentNode) {
        this.container.parentNode.removeChild(this.container);
      }

      // 清除引用
      this.container = null;
      this.button = null;
      this.window = null;
      this.iframe = null;
      this.config = null;
      this.isInitialized = false;
      this.isOpen = false;

      console.log('[ShengongChat] Widget 已销毁');
    }
  }

  // 创建全局实例
  let widgetInstance = null;

  /**
   * 公开 API
   */
  window.ShengongChat = {
    /**
     * 初始化聊天组件
     * @param {Object} options 配置选项
     * @returns {ShengongChatWidget} Widget 实例
     */
    init: function(options) {
      if (widgetInstance) {
        console.warn('[ShengongChat] 已存在实例，先销毁旧实例');
        widgetInstance.destroy();
      }

      widgetInstance = new ShengongChatWidget();
      return widgetInstance.init(options);
    },

    /**
     * 打开聊天窗口
     */
    open: function() {
      if (widgetInstance) {
        widgetInstance.open();
      } else {
        console.warn('[ShengongChat] 请先初始化 Widget');
      }
    },

    /**
     * 关闭聊天窗口
     */
    close: function() {
      if (widgetInstance) {
        widgetInstance.close();
      }
    },

    /**
     * 销毁 Widget
     */
    destroy: function() {
      if (widgetInstance) {
        widgetInstance.destroy();
        widgetInstance = null;
      }
    },

    /**
     * 获取当前实例
     */
    getInstance: function() {
      return widgetInstance;
    },

    /**
     * 版本号
     */
    version: '1.0.0'
  };

  console.log('[ShengongChat] SDK 加载完成，版本:', window.ShengongChat.version);

})(window, document);
