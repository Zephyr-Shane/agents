/**
 * 应用配置
 * 修改 API_BASE_URL 来指定后端服务地址
 * 优先级：环境变量 > 配置文件默认值
 */

// 尝试从环境变量读取（Vite 模式）
let apiBase
try {
  if (typeof import.meta !== 'undefined' && import.meta.env && import.meta.env.VITE_API_BASE) {
    apiBase = import.meta.env.VITE_API_BASE
  }
} catch {}

// 默认值（可修改此行来改变默认后端地址）
if (!apiBase) {
  apiBase = 'http://localhost:8080/api'
}

export const API_BASE_URL = apiBase
