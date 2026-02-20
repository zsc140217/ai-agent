<template>
  <div class="home-container">
    <div class="header">
      <div class="glitch-wrapper">
        <!-- 1. 修改标题为 JBLMJ -->
        <h1 class="glitch-title" data-text="JBLMJ AI超级智能体">JBLMJ AI超级智能体</h1>
      </div>
      <p class="subtitle">/ 基于 MCP 协议与 ReAct 引擎 /</p>
      <div class="cyber-line"></div>
    </div>
    
    <div class="apps-container">
      <!-- 2. 修改恋爱大师 -> 企业出差管家 -->
      <div class="app-card" @click="navigateTo('/enterprise-assistant')">
        <div class="card-glow"></div>
        <!-- 修改图标为商务公文包 -->
        <div class="app-icon enterprise-icon">💼</div>
        <div class="app-info">
          <div class="app-title">企业出差管家</div>
          <div class="app-desc">集成高德 MCP 与 RAG 知识库，智能规划外勤行程</div>
        </div>
        <div class="app-button">
          <span class="btn-text">进入办公空间</span>
          <span class="btn-icon">→</span>
        </div>
      </div>
      
      <div class="app-card" @click="navigateTo('/super-agent')">
        <div class="card-glow"></div>
        <div class="app-icon robot-icon">🤖</div>
        <div class="app-info">
          <div class="app-title">AI超级智能体</div>
          <div class="app-desc">自主规划引擎，多工具协同解决复杂任务</div>
        </div>
        <div class="app-button">
          <span class="btn-text">立即体验</span>
          <span class="btn-icon">→</span>
        </div>
      </div>
    </div>
    
    <div class="cyber-circles">
      <div class="circle circle-1"></div>
      <div class="circle circle-2"></div>
      <div class="circle circle-3"></div>
    </div>
    
    <AppFooter />
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { useHead } from '@vueuse/head'
import AppFooter from '../components/AppFooter.vue'

// 3. 修改 SEO 元数据
useHead({
  title: 'JBLMJ AI超级智能体应用平台 - 首页',
  meta: [
    {
      name: 'description',
      content: 'JBLMJ AI超级智能体应用平台提供企业出差管家和AI全能助手服务，利用RAG与MCP技术赋能企业生产力'
    },
    {
      name: 'keywords',
      content: 'JBLMJ,AI智能体,MCP协议,高德地图AI,企业级RAG,出差规划,AI助手'
    }
  ]
})

const router = useRouter()

const navigateTo = (path) => {
  router.push(path)
}
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Orbitron:wght@400;500;700&display=swap');

:root {
  --neon-blue: #00f0ff;
  --neon-purple: #9000ff;
  --neon-gold: #ffcc00; /* 新增金黄色调，增加商务感 */
  --cyber-black: #0a0a12;
  --cyber-dark: #111122;
  --cyber-light: #edf7ff;
}

.home-container {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  background-color: #0a0a12;
  background-image: 
    linear-gradient(0deg, rgba(8, 17, 34, 0.95), rgba(5, 8, 20, 0.95)),
    url('data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" width="100" height="100" viewBox="0 0 100 100"><rect x="0" y="0" width="100" height="1" fill="%23111133" opacity="0.3"/><rect x="0" y="0" width="1" height="100" fill="%23111133" opacity="0.3"/></svg>');
  background-size: auto, 40px 40px;
  position: relative;
  overflow: hidden;
}

.header {
  padding: 80px 20px 40px;
  text-align: center;
  position: relative;
  z-index: 2;
}

.glitch-wrapper {
  position: relative;
  display: inline-block;
}

.glitch-title {
  font-family: 'Orbitron', sans-serif;
  font-size: 3.5rem;
  font-weight: 800;
  color: #fff;
  text-transform: uppercase;
  text-shadow: 0 0 10px var(--neon-blue);
  animation: glitch 3s infinite;
}

/* 4. 修改伪元素内容为 JBLMJ */
.glitch-title::before,
.glitch-title::after {
  content: 'JBLMJ AI超级智能体';
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: #0a0a12;
}

.glitch-title::before {
  left: 2px;
  text-shadow: -2px 0 #ff00c1;
  clip: rect(44px, 450px, 56px, 0);
  animation: glitch-anim 5s infinite linear alternate-reverse;
}

.glitch-title::after {
  left: -2px;
  text-shadow: -2px 0 #00fff9, 2px 2px #ff00c1;
  animation: glitch-anim-2 1s infinite linear alternate-reverse;
}

.subtitle {
  font-family: 'Orbitron', sans-serif;
  font-size: 1rem;
  color: var(--neon-blue);
  margin-top: 10px;
  letter-spacing: 5px;
  opacity: 0.8;
}

.apps-container {
  display: flex;
  justify-content: center;
  gap: 40px;
  max-width: 1200px;
  margin: 40px auto;
  padding: 0 20px;
  z-index: 2;
}

.app-card {
  width: 380px;
  background: rgba(20, 20, 35, 0.6);
  border: 1px solid rgba(0, 240, 255, 0.2);
  border-radius: 20px;
  padding: 40px 30px;
  cursor: pointer;
  transition: all 0.3s ease;
  backdrop-filter: blur(10px);
}

.app-card:hover {
  transform: translateY(-10px);
  border-color: var(--neon-blue);
  box-shadow: 0 0 30px rgba(0, 240, 255, 0.2);
}

.app-icon {
  font-size: 3.5rem;
  margin-bottom: 20px;
}

/* 修改卡片图标渐变色 - 偏向商务蓝金 */
.enterprise-icon {
  background: linear-gradient(135deg, #0052cc, #00f0ff);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  filter: drop-shadow(0 0 10px rgba(0, 82, 204, 0.5));
}

.robot-icon {
  background: linear-gradient(135deg, #9000ff, #00f0ff);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.app-title {
  font-size: 1.8rem;
  font-weight: 700;
  color: #fff;
  margin-bottom: 15px;
}

.app-desc {
  color: rgba(255, 255, 255, 0.6);
  line-height: 1.6;
  margin-bottom: 25px;
}

.app-button {
  display: flex;
  align-items: center;
  justify-content: center;
  background: transparent;
  border: 1px solid var(--neon-blue);
  color: var(--neon-blue);
  padding: 10px 20px;
  border-radius: 5px;
  font-family: 'Orbitron', sans-serif;
  transition: all 0.3s;
}

.app-card:hover .app-button {
  background: var(--neon-blue);
  color: #000;
  box-shadow: 0 0 20px var(--neon-blue);
}

/* 装饰性动画圆圈 */
.circle {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  z-index: 1;
  opacity: 0.2;
}

.circle-1 { top: 10%; left: 10%; width: 300px; height: 300px; background: var(--neon-blue); }
.circle-2 { bottom: 10%; right: 10%; width: 400px; height: 400px; background: var(--neon-purple); }

/* 复用原有的关键帧动画 */
@keyframes glitch {
  0% { text-shadow: 2px 0 var(--neon-blue), -2px 0 var(--neon-purple); }
  100% { text-shadow: -2px 0 var(--neon-blue), 2px 0 var(--neon-purple); }
}

@keyframes glitch-anim {
  0% { clip: rect(31px, 9999px, 94px, 0); }
  100% { clip: rect(62px, 9999px, 43px, 0); }
}

@media (max-width: 768px) {
  .apps-container { flex-direction: column; align-items: center; }
  .glitch-title { font-size: 2.2rem; }
}
</style>