<template>
  <div class="enterprise-assistant-container">
    <div class="header">
      <div class="back-button" @click="goBack">返回</div>
      <h1 class="title">企业出差/外勤助手（增强版）</h1>
      <div class="header-info">
        <div class="chat-id">会话ID: {{ chatId }}</div>
        <div class="user-id">用户ID: {{ userId }}</div>
      </div>
    </div>

    <!-- 控制面板 -->
    <div class="control-panel">
      <div class="control-section">
        <label class="control-label">执行模式：</label>
        <div class="mode-selector">
          <button
            :class="['mode-btn', { active: executionMode === 'default' }]"
            @click="executionMode = 'default'"
          >
            🚀 快速模式
            <span class="mode-desc">复杂度评估 + 工具路由</span>
          </button>
          <button
            :class="['mode-btn', { active: executionMode === 'thinking' }]"
            @click="executionMode = 'thinking'"
          >
            🧠 思考模式
            <span class="mode-desc">ReAct循环推理</span>
          </button>
        </div>
      </div>

      <div class="control-section">
        <label class="control-label">记忆功能：</label>
        <div class="memory-actions">
          <button class="action-btn" @click="viewWorkingMemory">
            📝 查看工作记忆
          </button>
          <button class="action-btn" @click="viewUserProfile">
            👤 查看用户画像
          </button>
          <button class="action-btn primary" @click="triggerLearning">
            🎓 触发学习
          </button>
          <button class="action-btn danger" @click="clearMemory">
            🗑️ 清空记忆
          </button>
        </div>
      </div>
    </div>

    <div class="content-wrapper">
      <!-- 左侧：聊天区域 -->
      <div class="chat-area">
        <ChatRoom
          :messages="messages"
          :connection-status="connectionStatus"
          ai-type="enterprise"
          @send-message="sendMessage"
        />
      </div>

      <!-- 右侧：记忆面板 -->
      <div class="memory-panel" v-if="showMemoryPanel">
        <div class="panel-header">
          <h3>{{ memoryPanelTitle }}</h3>
          <button class="close-btn" @click="showMemoryPanel = false">✕</button>
        </div>
        <div class="panel-content">
          <pre v-if="memoryData">{{ JSON.stringify(memoryData, null, 2) }}</pre>
          <div v-else class="empty-state">暂无数据</div>
        </div>
      </div>
    </div>

    <!-- 状态提示 -->
    <div v-if="statusMessage" :class="['status-toast', statusType]">
      {{ statusMessage }}
    </div>

    <div class="footer-container">
      <div class="custom-footer">
        <div class="footer-links">
          <span>项目开源地址：</span>
          <a href="https://github.com/zsc140217/ai-agent" target="_blank">https://github.com/zsc140217/ai-agent</a>
        </div>
        <div class="footer-tips">
          💡 提示：系统会自动提取实体（城市、客户）和意图（查天气、订酒店），支持上下文理解
        </div>
      </div>
      <AppFooter />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { useHead } from '@vueuse/head'
import ChatRoom from '../components/ChatRoom.vue'
import AppFooter from '../components/AppFooter.vue'
import {
  chatWithEnterpriseApp,
  getWorkingMemory,
  getUserProfile,
  triggerLearning as apiTriggerLearning,
  clearConversation
} from '../api'

// 设置页面标题和元数据
useHead({
  title: '企业出差管家（增强版） - 三层记忆系统',
  meta: [
    {
      name: 'description',
      content: '企业出差助手增强版，集成三层记忆系统，支持上下文理解和个性化推荐。'
    },
    {
      name: 'keywords',
      content: 'AI出差助手,记忆系统,上下文理解,个性化推荐,工具路由,思考模式'
    }
  ]
})

const router = useRouter()
const messages = ref([])
const chatId = ref('')
const userId = ref('demo_user')
const connectionStatus = ref('disconnected')
const executionMode = ref('default') // 'default' 或 'thinking'
let eventSource = null

// 记忆面板
const showMemoryPanel = ref(false)
const memoryPanelTitle = ref('')
const memoryData = ref(null)

// 状态提示
const statusMessage = ref('')
const statusType = ref('info') // 'info', 'success', 'error'

// 生成随机会话ID
const generateChatId = () => {
  return 'trip_' + Math.random().toString(36).substring(2, 10)
}

// 生成用户ID（实际项目中应该从登录系统获取）
const generateUserId = () => {
  // 从localStorage获取或生成新的
  let storedUserId = localStorage.getItem('demo_user_id')
  if (!storedUserId) {
    storedUserId = 'user_' + Math.random().toString(36).substring(2, 10)
    localStorage.setItem('demo_user_id', storedUserId)
  }
  return storedUserId
}

const addMessage = (content, isUser) => {
  messages.value.push({
    content,
    isUser,
    time: new Date().getTime()
  })
}

const showStatus = (message, type = 'info') => {
  statusMessage.value = message
  statusType.value = type
  setTimeout(() => {
    statusMessage.value = ''
  }, 3000)
}

const sendMessage = (message) => {
  addMessage(message, true)

  if (eventSource) {
    eventSource.close()
  }

  const aiMessageIndex = messages.value.length
  addMessage('', false)

  connectionStatus.value = 'connecting'
  let isDone = false // 标记是否正常完成

  // 根据执行模式选择不同的API
  if (executionMode.value === 'default') {
    // 快速模式：使用SSE流式
    eventSource = chatWithEnterpriseApp(message, chatId.value, userId.value)

    eventSource.onmessage = (event) => {
      const data = event.data
      if (data && data !== '[DONE]') {
        if (aiMessageIndex < messages.value.length) {
          messages.value[aiMessageIndex].content += data
        }
      }

      if (data === '[DONE]') {
        isDone = true
        connectionStatus.value = 'disconnected'
        eventSource.close()
        showStatus('✅ 回复完成（快速模式）', 'success')
      }
    }

    eventSource.onerror = (error) => {
      // 如果已经正常完成，忽略关闭时的错误事件
      if (isDone) {
        return
      }
      console.error('SSE Error:', error)
      connectionStatus.value = 'error'
      eventSource.close()
      showStatus('❌ 连接错误', 'error')
    }
  } else {
    // 思考模式：使用同步API（暂时用SSE，后续可以改为同步）
    eventSource = chatWithEnterpriseApp(message, chatId.value, userId.value)

    eventSource.onmessage = (event) => {
      const data = event.data
      if (data && data !== '[DONE]') {
        if (aiMessageIndex < messages.value.length) {
          messages.value[aiMessageIndex].content += data
        }
      }

      if (data === '[DONE]') {
        isDone = true
        connectionStatus.value = 'disconnected'
        eventSource.close()
        showStatus('✅ 回复完成（思考模式）', 'success')
      }
    }

    eventSource.onerror = (error) => {
      // 如果已经正常完成，忽略关闭时的错误事件
      if (isDone) {
        return
      }
      console.error('SSE Error:', error)
      connectionStatus.value = 'error'
      eventSource.close()
      showStatus('❌ 连接错误', 'error')
    }
  }
}

// 查看工作记忆
const viewWorkingMemory = async () => {
  try {
    const data = await getWorkingMemory(chatId.value)
    memoryData.value = data
    memoryPanelTitle.value = '工作记忆（当前会话）'
    showMemoryPanel.value = true
    showStatus('✅ 工作记忆加载成功', 'success')
  } catch (error) {
    console.error('获取工作记忆失败:', error)
    showStatus('❌ 获取工作记忆失败', 'error')
  }
}

// 查看用户画像
const viewUserProfile = async () => {
  try {
    const data = await getUserProfile(userId.value)
    memoryData.value = data
    memoryPanelTitle.value = '用户画像（长期记忆）'
    showMemoryPanel.value = true
    showStatus('✅ 用户画像加载成功', 'success')
  } catch (error) {
    console.error('获取用户画像失败:', error)
    showStatus('❌ 获取用户画像失败', 'error')
  }
}

// 触发学习
const triggerLearning = async () => {
  try {
    await apiTriggerLearning(userId.value, chatId.value)
    showStatus('✅ 学习完成！用户画像已更新', 'success')
  } catch (error) {
    console.error('触发学习失败:', error)
    showStatus('❌ 触发学习失败', 'error')
  }
}

// 清空记忆
const clearMemory = async () => {
  if (!confirm('确定要清空当前会话的记忆吗？')) {
    return
  }

  try {
    await clearConversation(chatId.value)
    showStatus('✅ 会话记忆已清空', 'success')
  } catch (error) {
    console.error('清空记忆失败:', error)
    showStatus('❌ 清空记忆失败', 'error')
  }
}

const goBack = () => {
  router.push('/')
}

onMounted(() => {
  chatId.value = generateChatId()
  userId.value = generateUserId()

  // 欢迎语
  addMessage(`您好，我是您的企业出差管家（增强版）。

🎯 当前功能：
• 三层记忆系统：自动提取实体和意图，支持上下文理解
• 执行模式切换：快速模式（工具路由）vs 思考模式（ReAct推理）
• 个性化推荐：基于历史行为，提供定制化服务

💡 试试说：
1. "我要去上海出差，帮我查一下天气"（系统会自动提取"上海"和"查询天气"）
2. "那边有什么酒店"（系统理解"那边"指的是"上海"）
3. 点击"触发学习"更新用户画像
4. 点击"查看工作记忆"查看提取的实体和意图

请问有什么可以帮您？`, false)
})

onBeforeUnmount(() => {
  if (eventSource) {
    eventSource.close()
  }
})
</script>

<style scoped>
.enterprise-assistant-container {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  background-color: #f4f7f9;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 24px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
  position: sticky;
  top: 0;
  z-index: 10;
}

.back-button {
  font-size: 16px;
  cursor: pointer;
  display: flex;
  align-items: center;
  transition: opacity 0.2s;
}

.back-button:hover {
  opacity: 0.8;
}

.back-button:before {
  content: '←';
  margin-right: 8px;
}

.title {
  font-size: 20px;
  font-weight: 600;
  margin: 0;
}

.header-info {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  font-size: 12px;
  opacity: 0.9;
}

.chat-id, .user-id {
  margin: 2px 0;
}

/* 控制面板 */
.control-panel {
  background: white;
  padding: 16px 24px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  display: flex;
  gap: 24px;
  flex-wrap: wrap;
}

.control-section {
  display: flex;
  align-items: center;
  gap: 12px;
}

.control-label {
  font-weight: 600;
  color: #333;
  white-space: nowrap;
}

.mode-selector {
  display: flex;
  gap: 12px;
}

.mode-btn {
  padding: 10px 16px;
  border: 2px solid #e0e0e0;
  border-radius: 8px;
  background: white;
  cursor: pointer;
  transition: all 0.3s;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  min-width: 160px;
}

.mode-btn:hover {
  border-color: #667eea;
  transform: translateY(-2px);
  box-shadow: 0 4px 8px rgba(102, 126, 234, 0.2);
}

.mode-btn.active {
  border-color: #667eea;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.mode-desc {
  font-size: 11px;
  opacity: 0.8;
  margin-top: 4px;
}

.memory-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.action-btn {
  padding: 8px 16px;
  border: 1px solid #e0e0e0;
  border-radius: 6px;
  background: white;
  cursor: pointer;
  transition: all 0.2s;
  font-size: 14px;
}

.action-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.action-btn.primary {
  background: #667eea;
  color: white;
  border-color: #667eea;
}

.action-btn.primary:hover {
  background: #5568d3;
}

.action-btn.danger {
  background: #f56565;
  color: white;
  border-color: #f56565;
}

.action-btn.danger:hover {
  background: #e53e3e;
}

/* 内容区域 */
.content-wrapper {
  display: flex;
  flex: 1;
  gap: 16px;
  padding: 16px;
  position: relative;
}

.chat-area {
  flex: 1;
  min-width: 0;
}

/* 记忆面板 */
.memory-panel {
  width: 400px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  display: flex;
  flex-direction: column;
  max-height: calc(100vh - 200px);
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  border-bottom: 1px solid #e0e0e0;
}

.panel-header h3 {
  margin: 0;
  font-size: 16px;
  color: #333;
}

.close-btn {
  background: none;
  border: none;
  font-size: 20px;
  cursor: pointer;
  color: #999;
  padding: 0;
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 4px;
  transition: all 0.2s;
}

.close-btn:hover {
  background: #f0f0f0;
  color: #333;
}

.panel-content {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
}

.panel-content pre {
  margin: 0;
  font-size: 12px;
  line-height: 1.6;
  color: #333;
  white-space: pre-wrap;
  word-wrap: break-word;
}

.empty-state {
  text-align: center;
  color: #999;
  padding: 40px 20px;
}

/* 状态提示 */
.status-toast {
  position: fixed;
  top: 80px;
  right: 24px;
  padding: 12px 20px;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  z-index: 1000;
  animation: slideIn 0.3s ease-out;
  font-size: 14px;
}

.status-toast.info {
  background: #3182ce;
  color: white;
}

.status-toast.success {
  background: #38a169;
  color: white;
}

.status-toast.error {
  background: #e53e3e;
  color: white;
}

@keyframes slideIn {
  from {
    transform: translateX(400px);
    opacity: 0;
  }
  to {
    transform: translateX(0);
    opacity: 1;
  }
}

/* Footer */
.footer-container {
  margin-top: auto;
}

.custom-footer {
  background-color: #ffffff;
  padding: 20px;
  text-align: center;
  border-top: 1px solid #e1e4e8;
}

.footer-links {
  font-size: 14px;
  color: #586069;
  margin-bottom: 8px;
}

.footer-links a {
  color: #0366d6;
  text-decoration: none;
  font-weight: 500;
}

.footer-links a:hover {
  text-decoration: underline;
}

.footer-tips {
  font-size: 12px;
  color: #999;
  margin-top: 8px;
}

/* 响应式 */
@media (max-width: 1200px) {
  .memory-panel {
    width: 320px;
  }
}

@media (max-width: 968px) {
  .content-wrapper {
    flex-direction: column;
  }

  .memory-panel {
    width: 100%;
    max-height: 400px;
  }

  .control-panel {
    flex-direction: column;
    gap: 16px;
  }

  .control-section {
    flex-direction: column;
    align-items: flex-start;
    width: 100%;
  }

  .mode-selector, .memory-actions {
    width: 100%;
  }

  .mode-btn {
    flex: 1;
  }
}

@media (max-width: 768px) {
  .header {
    padding: 12px 16px;
  }

  .title {
    font-size: 16px;
  }

  .header-info {
    font-size: 10px;
  }

  .control-panel {
    padding: 12px 16px;
  }

  .mode-btn {
    min-width: 120px;
    padding: 8px 12px;
    font-size: 13px;
  }

  .action-btn {
    font-size: 12px;
    padding: 6px 12px;
  }
}
</style>
