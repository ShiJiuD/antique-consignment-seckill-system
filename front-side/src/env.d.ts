/// <reference types="vite/client" />

/**
 * Vue 单文件组件（.vue）的 TypeScript 模块声明
 *
 * 作用：让 TS 能够识别并正确推断 .vue 文件的导入类型，
 * 否则 `import Foo from '@/views/Foo/index.vue'` 会报"找不到模块"。
 */
declare module '*.vue' {
  import type { DefineComponent } from 'vue'
  const component: DefineComponent<object, object, unknown>
  export default component
}
