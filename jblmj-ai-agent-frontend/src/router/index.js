import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    name: 'Home',
    component: () => import('../views/Home.vue'),
    meta: {
      title: '首页 - JBLMJ AI超级智能体应用平台',
      description: 'JBLMJ AI超级智能体应用平台提供企业出差管家和AI超级智能体服务，满足您的企业办公与智能对话需求'
    }
  },
  {
    path: '/enterprise-assistant',
    name: 'EnterpriseAssistant',
    component: () => import('../views/EnterpriseAssistant.vue'),
    meta: {
      title: '企业出差管家 - JBLMJ AI超级智能体应用平台',
      description: '企业出差管家是专业的智能外勤助手，集成高德MCP与企业RAG知识库，为您提供精准的差旅规划服务'
    }
  },
  {
    path: '/super-agent',
    name: 'SuperAgent',
    component: () => import('../views/SuperAgent.vue'),
    meta: {
      title: 'AI超级智能体 - JBLMJ AI超级智能体应用平台',
      description: 'AI超级智能体是基于自主规划引擎的全能型AI助手，能多工具协作解决各类专业问题'
    }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 全局导航守卫，设置文档标题
router.beforeEach((to, from, next) => {
  // 设置页面标题
  if (to.meta.title) {
    document.title = to.meta.title
  }
  next()
})

export default router