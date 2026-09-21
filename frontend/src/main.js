/**
 * @file main entry
 */

import Vue from 'vue'
import LocalAtom from './data/LocalAtom'
import PublicAtom from './data/PublicAtom'
import bkMagic from 'bk-magic-vue'
import bkciAtoms from 'bkci-atom-components'
import VeeValidate from 'vee-validate'
import request from '@/utils/request'

require('./css/conf.scss')
// 全量引入 bk-magic-vue 样式
require('bk-magic-vue/dist/bk-magic-vue.min.css')

Vue.use(bkMagic)
Vue.use(bkciAtoms)

Vue.prototype.$ajax = request

Vue.use(VeeValidate, {
    fieldsBagName: 'veeFields',
    locale: 'cn'
})

// 🚨 暴力环境判定：只要在 8001 端口或者是本地域名，就强制认为是本地开发
const isLocal = (
    window.location.port === '8001' || 
    window.location.hostname === 'localhost' || 
    window.location.hostname === '127.0.0.1'
)

// 强制在 Console 输出（警告级别，防止被过滤）
console.warn('[BKCI-ATOM] ENTRY LOADED')
console.warn('URL:', window.location.href)
console.warn('IS_LOCAL:', isLocal)
console.warn('RENDER_TARGET:', isLocal ? 'LocalAtom' : 'PublicAtom')

// 挂载到全局，方便手动调试
window.__ATOM_ENV__ = { isLocal, version: '1.0.0' }

global.atomVue = new Vue({
    el: '#pipeline-atom',
    components: {
        PublicAtom,
        LocalAtom
    },
    template: isLocal ? '<LocalAtom/>' : '<PublicAtom/>'
})
