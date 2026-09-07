import { createApp } from 'vue'

/**
 * Element Plus —— 完整引入
 * 注意：完整引入会打包全部组件，体积较大（~1MB gzip 后约 200KB）。
 * 对体积敏感时可按需引入，详见：https://element-plus.org/zh-CN/guide/quickstart.html
 */
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'

/**
 * 全局样式（Vite 脚手架自带）
 */
import './style.css'

/**
 * 根组件
 */
import App from './App.vue'

/**
 * 路由实例
 * 内部已包含 beforeEach 全局守卫：白名单 ['/login'] 直接放行，其余页面需 token
 */
import router from './router'

// 创建 Vue 应用实例
const app = createApp(App)

/**
 * 全局注册插件
 * app.use() —— Vue3 的标准插件注册方式
 */
app.use(ElementPlus) // Element Plus UI 组件库（全局注册所有组件）
app.use(router)      // vue-router 路由

// 挂载到 #app 根节点（index.html 中的 <div id="app"></div>）
app.mount('#app')
