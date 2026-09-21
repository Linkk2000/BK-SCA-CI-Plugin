/**
 * 环境配置 - 备用方案
 * 如果 webpack 的 ISLOCAL 不工作，使用这个
 */

// 检测是否为本地开发环境
export const isLocalDevelopment = () => {
    // 方法 1：检查 hostname
    if (typeof window !== 'undefined') {
        const hostname = window.location.hostname
        if (hostname === 'localhost' || hostname === '127.0.0.1') {
            return true
        }
    }
    
    // 方法 2：检查 webpack 的 ISLOCAL
    if (typeof ISLOCAL !== 'undefined') {
        return ISLOCAL === true
    }
    
    // 方法 3：检查 process.env（如果可用）
    if (typeof process !== 'undefined' && process.env && process.env.NODE_ENV) {
        return process.env.NODE_ENV === 'development'
    }
    
    // 默认返回 false（生产环境）
    return false
}

export const IS_LOCAL = isLocalDevelopment()

