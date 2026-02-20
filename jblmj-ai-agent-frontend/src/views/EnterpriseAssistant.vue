<template>
  <div class="enterprise-assistant-container">
    <div class="header">
      <div class="back-button" @click="goBack">返回</div>
      <h1 class="title">企业出差/外勤助手</h1>
      <div class="chat-id">会话ID: {{ chatId }}</div>
    </div>
    
    <div class="content-wrapper">
      <div class="chat-area">
        <ChatRoom 
          :messages="messages" 
          :connection-status="connectionStatus"
          ai-type="enterprise"
          @send-message="sendMessage"
        />
      </div>
    </div>
    
    <div class="footer-container">
      <div class="custom-footer">
        <div class="footer-links">
          <span>项目开源地址：</span>
          <a href="https://github.com/zsc140217/ai-agent" target="_blank">https://github.com/zsc140217/ai-agent</a>
        </div>
      </div>
      <!-- 如果你有统一的 AppFooter 组件，也可以保留 -->
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
import { chatWithEnterpriseApp } from '../api'

// 设置页面标题和元数据（更有利于简历展示）
useHead({
  title: '企业出差管家 - 智能外勤助手',
  meta: [
    {
      name: 'description',
      content: '企业出差助手是基于智能体技术的专业管家，提供差旅制度查询、协议酒店推荐及行程规划服务。'
    },
    {
      name: 'keywords',
      content: 'AI出差助手,行程规划,MCP地图,RAG知识库,企业提效工具,智能体应用'
    }
  ]
})

const router = useRouter()
const messages = ref([])
const chatId = ref('')
const connectionStatus = ref('disconnected')
let eventSource = null

// 生成随机会话ID (前缀改为 trip)
const generateChatId = () => {
  return 'trip_' + Math.random().toString(36).substring(2, 10)
}

const addMessage = (content, isUser) => {
  messages.value.push({
    content,
    isUser,
    time: new Date().getTime()
  })
}

const sendMessage = (message) => {
  addMessage(message, true)
  
  if (eventSource) {
    eventSource.close()
  }
  
  const aiMessageIndex = messages.value.length
  addMessage('', false)
  
  connectionStatus.value = 'connecting'
  // 调用后端接口
  eventSource = chatWithEnterpriseApp(message, chatId.value) 
  
  eventSource.onmessage = (event) => {
    const data = event.data
    if (data && data !== '[DONE]') {
      if (aiMessageIndex < messages.value.length) {
        messages.value[aiMessageIndex].content += data
      }
    }
    
    if (data === '[DONE]') {
      connectionStatus.value = 'disconnected'
      eventSource.close()
    }
  }
  
  eventSource.onerror = (error) => {
    console.error('SSE Error:', error)
    connectionStatus.value = 'error'
    eventSource.close()
  }
}

const goBack = () => {
  router.push('/')
}

onMounted(() => {
  chatId.value = generateChatId()
  // 修改欢迎语为人设对话
  addMessage('您好，我是您的企业出差管家。我可以帮您查询公司报销制度、规划行程路线或查看客户地址。请问有什么可以帮您？', false)
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
  /* 背景改为淡雅的商务灰蓝 */
  background-color: #f4f7f9;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 24px;
  /* 颜色改为商务蓝 */
  background-color: #0052cc;
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

.chat-id {
  font-size: 14px;
  opacity: 0.8;
}

.content-wrapper {
  display: flex;
  flex-direction: column;
  flex: 1;
}

.chat-area {
  flex: 1;
  padding: 16px;
  overflow: hidden;
  position: relative;
  min-height: calc(100vh - 56px - 140px);
  margin-bottom: 16px;
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
}

.footer-links a {
  color: #0366d6;
  text-decoration: none;
  font-weight: 500;
}

.footer-links a:hover {
  text-decoration: underline;
}

.footer-container {
  margin-top: auto;
}

@media (max-width: 768px) {
  .header { padding: 12px 16px; }
  .title { font-size: 18px; }
  .chat-area { min-height: calc(100vh - 48px - 120px); }
}
</style>