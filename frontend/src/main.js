/**
 * @file main entry
 */

console.log('🚀 Plugin JS Loaded - BKCI Custom Atom')

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
// 如需用到代码编辑组件atom-ace-editor时需引用，则需要把这行注释去掉
// require('bkci-atom-components/dist/brace.js')

Vue.use(bkMagic)
Vue.use(bkciAtoms)

Vue.prototype.$ajax = request

Vue.use(VeeValidate, {
    fieldsBagName: 'veeFields',
    locale: 'cn'
})

global.atomVue = new Vue({
    el: '#pipeline-atom',
    components: {
        PublicAtom,
        LocalAtom
    },
    template: `${ISLOCAL ? '<LocalAtom/>' : '<PublicAtom/>'}`
})
