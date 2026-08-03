import axios from 'axios'

// 创建axios实例
const request = axios.create({
  baseURL: '', // 后端服务地址，后面联调再填写，例如 http://localhost:8080
  timeout: 10000
})

// 请求拦截器：每次请求自动带上token
request.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// 响应拦截器：直接取出后端返回data，统一处理报错
request.interceptors.response.use(
  (res) => res.data,
  (err) => {
    console.error('请求出错：', err)
    return Promise.reject(err)
  }
)

export default request