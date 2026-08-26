import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw, NavigationGuardNext, RouteLocationNormalized } from 'vue-router'

/**
 * 路由配置表
 * 使用 RouteRecordRaw 类型约束，确保路由定义的类型安全
 */
const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login/index.vue'),
    meta: {
      title: '登录',
      requiresAuth: false,
    },
  },
  {
    path: '/home',
    name: 'Home',
    component: () => import('@/views/Home/index.vue'),
    meta: {
      title: '首页',
      requiresAuth: true,
    },
  },
  {
    path: '/search',
    name: 'Search',
    component: () => import('@/views/Search/index.vue'),
    meta: {
      title: '搜索藏品',
      requiresAuth: true,
    },
  },
  {
    path: '/antique/:id',
    name: 'AntiqueDetail',
    component: () => import('@/views/Detail/index.vue'),
    meta: {
      title: '藏品详情',
      requiresAuth: true,
    },
  },
  {
    path: '/discover',
    name: 'Discover',
    component: () => import('@/views/Discover/index.vue'),
    meta: {
      title: '发现',
      requiresAuth: true,
    },
  },
  {
    path: '/message',
    name: 'Message',
    component: () => import('@/views/Message/index.vue'),
    meta: {
      title: '消息',
      requiresAuth: true,
    },
  },
  {
    path: '/ai',
    name: 'Ai',
    component: () => import('@/views/Ai/index.vue'),
    meta: {
      title: 'AI智能助手',
      requiresAuth: true,
    },
  },
  {
    path: '/mine',
    name: 'Mine',
    component: () => import('@/views/Mine/index.vue'),
    meta: {
      title: '我的',
      requiresAuth: true,
    },
  },
  {
    path: '/favorite',
    name: 'Favorite',
    component: () => import('@/views/Favorite/index.vue'),
    meta: {
      title: '我的收藏',
      requiresAuth: true,
    },
  },
  {
    path: '/history',
    name: 'History',
    component: () => import('@/views/History/index.vue'),
    meta: {
      title: '浏览记录',
      requiresAuth: true,
    },
  },
  {
    path: '/order/list',
    name: 'OrderList',
    component: () => import('@/views/Order/List/index.vue'),
    meta: {
      title: '我的订单',
      requiresAuth: true,
    },
  },
  {
    path: '/order/detail/:id',
    name: 'OrderDetail',
    component: () => import('@/views/Order/Detail/index.vue'),
    meta: {
      title: '订单详情',
      requiresAuth: true,
    },
  },
  {
    path: '/order/pay',
    name: 'OrderPay',
    component: () => import('@/views/Order/Pay/index.vue'),
    meta: {
      title: '模拟支付',
      requiresAuth: true,
    },
  },
  {
    path: '/',
    redirect: '/login',
  },
]

/**
 * 创建路由实例
 * createWebHistory —— HTML5 History 模式，URL 不带 `#`
 */
const router = createRouter({
  history: createWebHistory(),
  routes,
})

/**
 * ------------------------------
 * 白名单路由
 * 这些路径无需登录 token 即可访问
 * ------------------------------
 */
const WHITE_LIST: string[] = ['/login']

/**
 * 从 localStorage 获取 token
 * token 键名统一为 `token`
 */
function getToken(): string | null {
  return localStorage.getItem('token')
}

/**
 * 全局前置守卫 —— beforeEach
 *
 * 职责：
 * 1. 动态设置页面标题（document.title）
 * 2. 校验当前路由是否需要登录 token
 * 3. 白名单路径直接放行
 * 4. 非白名单路径无 token → 重定向到 /login
 */
router.beforeEach(
  (
    to: RouteLocationNormalized,
    _from: RouteLocationNormalized,
    next: NavigationGuardNext,
  ) => {
    // 设置页面标题
    if (to.meta?.title) {
      document.title = to.meta.title as string
    }

    const token: string | null = getToken()

    // 白名单路由：直接放行
    if (WHITE_LIST.includes(to.path)) {
      // 如果已登录用户访问登录页，可在此处重定向到首页（可选）
      // if (token) { next('/home'); return; }
      next()
      return
    }

    // 非白名单路由：必须携带 token，否则跳转登录页
    if (!token) {
      /**
       * 跳转时通过 query 携带重定向地址，便于登录成功后回跳
       *
       * 注意：token 在 HTTP 请求头中应以 Bearer 格式携带，
       * 示例：`Authorization: Bearer <token>`
       * 该逻辑通常在 axios 请求拦截器中统一处理，此处不做额外操作。
       */
      next({ path: '/login', query: { redirect: to.fullPath } })
      return
    }

    // token 存在，正常放行
    next()
  },
)

export default router
