import axios from 'axios'
import Vue from 'vue'
import { bus } from './bus'
import cookie from 'cookie'

const request = axios.create({
    baseURL: '',
    validateStatus: status => {
        if (status > 400) {
            console.warn(`HTTP 请求出错 status: ${status}`)
        }
        return status >= 200 && status <= 503
    },
    withCredentials: true
})

function errorHandler (error) {
    return Promise.reject(error)
}

request.interceptors.request.use(config => {
    const url = new URL(config.url)
    if (!/(devops|gw\.open)\.oa\.com$/i.test(url.host) || /(\/?ms\/backend|\/?backend)\//i.test(url.pathname)) {
        return config
    }

    const routePid = getCurrentPid()
    return {
        ...config,
        headers: routePid ? {
            ...(config.headers || {}),
            'X-DEVOPS-PROJECT-ID': routePid
        } : config.headers
    }
}, function (error) {
    return Promise.reject(error)
})

// request.interceptors.response.use(response => {
//     injectCSRFTokenToHeaders() // 注入csrfToken
//     const { data: { status, message, code, result } } = response
//     const httpStatus = response.status
//     if (httpStatus === 401) {
//         bus.$toggleLoginDialog(true)
//     } else if (httpStatus === 503) {
//         const errMsg = {
//             status: httpStatus,
//             message: '服务正在部署中，请稍候...'
//         }
//         return Promise.reject(errMsg)
//     } else if ((typeof code !== 'undefined' && code !== 0) || (typeof status !== 'undefined' && status !== 0)) {
//         let msg = message
//         if (Object.prototype.toString.call(message) === '[object Object]') {
//             msg = Object.keys(message).map(key => message[key].join(';')).join(';')
//         } else if (Object.prototype.toString.call(message) === '[object Array]') {
//             msg = message.join(';')
//         }
//         const errorMsg = { httpStatus, message: msg, code: code || status }
//         return Promise.reject(errorMsg)
//     }
//     return response.data
// }, errorHandler)

request.interceptors.response.use(response => {
    injectCSRFTokenToHeaders() // 注入csrfToken
    const { data: { status, message, code, result } } = response
    const httpStatus = response.status
    if (httpStatus === 401) {
        bus.$toggleLoginDialog(true)
    } else if (httpStatus === 503) {
        const errMsg = {
            status: httpStatus,
            message: '服务正在部署中，请稍候...'
        }
        return Promise.reject(errMsg)
    } else if ((typeof status !== 'undefined' && status !== 0) || (typeof result !== 'undefined' && !result)) {
        const errorMsg = { httpStatus, message, code: code || status }
        return Promise.reject(errorMsg)
    } else if (httpStatus === 400) {
        const errorMsg = { httpStatus, message: '内部服务异常' }
        return Promise.reject(errorMsg)
    }

    return response.data
}, errorHandler)

const injectCSRFTokenToHeaders = () => {
    const CSRFToken = cookie.parse(document.cookie).backend_csrftoken
    if (CSRFToken !== undefined) {
        request.defaults.headers.post['X-CSRFToken'] = CSRFToken
    } else {
        console.warn('Can not find backend_csrftoken in document.cookie')
    }
}

const getCurrentPid = () => {
    try {
        const pathPid = window.pass3Vue && window.pass3Vue.$route && window.pass3Vue.$route.params && window.pass3Vue.$route.params.projectId
        const lsPid = localStorage.getItem('projectId')
        return pathPid || lsPid
    } catch (e) {
        return undefined
    }
}

Vue.prototype.$ajax = request

export default request
