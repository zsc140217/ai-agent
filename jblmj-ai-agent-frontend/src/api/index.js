import axios from 'axios'

// 根据环境变量设置 API 基础 URL
const API_BASE_URL = process.env.NODE_ENV === 'production'
 ? '/api' // 生产环境使用相对路径，适用于前后端部署在同一域名下
 : 'http://localhost:8123/api' // 开发环境指向本地后端服务

// 创建axios实例
const request = axios.create({
  baseURL: API_BASE_URL,
  timeout: 60000
})

// 封装SSE连接
export const connectSSE = (url, params, onMessage, onError) => {
  // 构建带参数的URL
  const queryString = Object.keys(params)
    .map(key => `${encodeURIComponent(key)}=${encodeURIComponent(params[key])}`)
    .join('&')

  const fullUrl = `${API_BASE_URL}${url}?${queryString}`

  // 创建EventSource
  const eventSource = new EventSource(fullUrl)

  eventSource.onmessage = event => {
    let data = event.data

    // 检查是否是特殊标记
    if (data === '[DONE]') {
      if (onMessage) onMessage('[DONE]')
    } else {
      // 处理普通消息
      if (onMessage) onMessage(data)
    }
  }

  eventSource.onerror = error => {
    if (onError) onError(error)
    eventSource.close()
  }

  // 返回eventSource实例，以便后续可以关闭连接
  return eventSource
}

// AI出差规划（SSE流式）
export const chatWithEnterpriseApp = (message, chatId, userId = 'anonymous') => {
  return connectSSE('/ai/enterprise/chat/sse', { message, chatId, userId })
}

// AI出差规划（带模式选择，同步）
export const chatWithMode = async (message, chatId, userId = 'anonymous', mode = 'default') => {
  const response = await request.get('/ai/enterprise/chat', {
    params: { message, chatId, userId, mode }
  })
  return response.data
}

// AI超级智能体聊天
export const chatWithManus = (message) => {
  return connectSSE('/ai/manus/chat', { message })
}

// ========== 记忆系统API ==========

// 获取工作记忆
export const getWorkingMemory = async (conversationId) => {
  const response = await request.get(`/memory/working/${conversationId}`)
  return response.data
}

// 获取用户画像
export const getUserProfile = async (userId) => {
  const response = await request.get(`/memory/profile/${userId}`)
  return response.data
}

// 触发学习（更新用户画像）
export const triggerLearning = async (userId, conversationId) => {
  const response = await request.post('/memory/learn', null, {
    params: { userId, conversationId }
  })
  return response.data
}

// 清空会话记忆
export const clearConversation = async (conversationId) => {
  const response = await request.delete(`/memory/conversation/${conversationId}`)
  return response.data
}

// 获取记忆系统统计
export const getMemoryStats = async () => {
  const response = await request.get('/memory/stats')
  return response.data
}

export default {
  chatWithEnterpriseApp,
  chatWithMode,
  chatWithManus,
  getWorkingMemory,
  getUserProfile,
  triggerLearning,
  clearConversation,
  getMemoryStats
}
