<template>
  <footer class="app-footer">
    <div class="footer-content">
      <!-- 品牌与愿景 -->
      <div class="footer-section">
        <div class="footer-logo">
          <h3>JBLMJ AI 智能体应用平台</h3>
        </div>
        <p class="footer-desc">
          探索基于 MCP 协议、ReAct 自主决策引擎与 Advanced RAG 技术的企业级 AI 解决方案，赋能数字化办公新范式。
        </p>
      </div>
      
      <!-- 项目资源 -->
      <div class="footer-section">
        <h4>项目资源</h4>
        <div class="footer-links">
          <a href="https://github.com/zsc140217/ai-agent" target="_blank" class="github-link">
            <span class="icon">🔗</span> GitHub 源码仓库
          </a>
          <span class="tech-stack">技术栈：Spring AI / PgVector / Vue 3</span>
        </div>
      </div>
      
      <!-- 关于作者 -->
      <div class="footer-section">
        <h4>关于项目</h4>
        <div class="footer-info">
          <p>开发者：zsc140217</p>
          <p>定位：企业级感知智能体引擎原型</p>
        </div>
      </div>
    </div>
    
    <div class="footer-bottom">
      <p>© {{ currentYear }} JBLMJ AI Agent Platform. 让智能触手可及</p>
    </div>
  </footer>
</template>

<script setup>
import { computed } from 'vue'

// 计算当前年份
const currentYear = computed(() => new Date().getFullYear())
</script>

<style scoped>
.app-footer {
  /* 使用半透明背景以适应赛博黑和商务白两种页面 */
  background-color: rgba(255, 255, 255, 0.05);
  backdrop-filter: blur(10px);
  padding: 50px 0 20px;
  color: #a0a0a0;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
  width: 100%;
  margin-top: auto;
  position: relative;
  z-index: 2;
}

.footer-content {
  max-width: 1200px;
  margin: 0 auto;
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  padding: 0 40px;
}

.footer-section {
  flex: 1;
  min-width: 250px;
  margin-bottom: 30px;
  padding: 0 20px;
}

.footer-logo h3 {
  font-size: 1.4rem;
  margin-bottom: 15px;
  color: #fff;
  letter-spacing: 1px;
}

.footer-desc {
  font-size: 0.9rem;
  line-height: 1.6;
  color: #888;
}

.footer-section h4 {
  font-size: 1.1rem;
  margin-bottom: 20px;
  color: #eee;
  font-weight: 600;
}

.footer-links {
  display: flex;
  flex-direction: column;
}

.footer-links a {
  margin-bottom: 12px;
  color: #00f0ff; /* 赛博蓝，突出链接 */
  text-decoration: none;
  transition: all 0.3s;
  font-weight: 500;
}

.footer-links a:hover {
  color: #fff;
  text-shadow: 0 0 10px #00f0ff;
}

.tech-stack {
  font-size: 0.85rem;
  color: #666;
  margin-top: 5px;
}

.footer-info p {
  margin-bottom: 10px;
  font-size: 0.9rem;
}

.footer-bottom {
  text-align: center;
  padding-top: 25px;
  margin-top: 30px;
  border-top: 1px solid rgba(255, 255, 255, 0.05);
  color: #555;
  font-size: 0.85rem;
  letter-spacing: 1px;
}

/* 针对内部白色页面的适配（如果背景是浅色） */
:deep(.enterprise-assistant-container) .app-footer {
  background-color: #f8f9fa;
  color: #666;
  border-top: 1px solid #eee;
}
:deep(.enterprise-assistant-container) .footer-logo h3,
:deep(.enterprise-assistant-container) .footer-section h4 {
  color: #333;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .footer-content {
    flex-direction: column;
    text-align: center;
  }
  
  .footer-section {
    width: 100%;
    margin-bottom: 30px;
    padding: 0;
  }

  .footer-links {
    align-items: center;
  }
}
</style>