<template>
    <div id="public-atom" v-resize="resize" class="remote-atom" :class="{ 'atom-disabled': atomDisabled }">
        <main class="app-container">
            <Atom
                v-if="hasInitData"
                :atom-props-value="atomPropsValue"
                :atom-props-model="atomPropsModel"
                :atom-props-container-info="containerInfo"
                :atom-props-disabled="atomDisabled"
                :current-user-info="currentUserInfo"
                :env-conf="envConf"
            >
            </Atom>
        </main>
    </div>
</template>

<script>
    import Vue from 'vue'
    import Atom from '../Atom'
    export default {
        name: 'public-atom',
        components: {
            Atom
        },
        directives: {
            resize: {
                bind(el, binding) { // el为绑定的元素，binding为绑定给指令的对象
                    let width = '', height = ''
                    function isReize() {
                        const style = document.defaultView.getComputedStyle(el);
                        if (width !== style.width || height !== style.height) {
                            binding.value()
                        }
                        width = style.width;
                        height = style.height
                    }
                    el.__vueSetInterval__ = setInterval(isReize, 200)
                },
                unbind(el) {
                    clearInterval(el.__vueSetInterval__)
                }
            }
        },
        data () {
            return {
                hasInitData: false,
                atomPropsValue: {},
                atomPropsModel: {},
                containerInfo: {},
                currentUserInfo: {},
                atomDisabled: false,
                envConf: {}
            }
        },

        // 脚手架在插件的created阶段订阅接收来自蓝盾上层的数据
        created () {
            console.log('Plugin created, waiting for parent message...')
            window.addEventListener('message', (e) => {
                console.log('Received message from parent:', {
                    origin: e.origin,
                    hasAtomPropsValue: !!(e.data && e.data.atomPropsValue),
                    hasAtomPropsModel: !!(e.data && e.data.atomPropsModel),
                    data: e.data
                })
                // 只处理来自父页面的数据消息，忽略其他消息
                if (!e.data || typeof e.data !== 'object') {
                    console.log('Skipping non-object message')
                    return
                }
                // atomPropsValue 可以是空对象 {}，atomPropsModel 必须存在
                if (e.data && e.data.hasOwnProperty('atomPropsValue') && e.data.atomPropsModel) {
                    // 插件相应字段的值
                    this.atomPropsValue = e.data.atomPropsValue
                    // task.json中定义的插件字段模型
                    this.atomPropsModel = e.data.atomPropsModel
                    // 插件所在的job的相关信息，如编译环境类型等
                    this.containerInfo = e.data.containerInfo || {}
                    // 当前访问用户的姓名、头像信息等
                    this.currentUserInfo = e.data.currentUserInfo || {}
                    // 当前插件是否可编辑，当在构建详情、预览、模板示例化出的流水线点开该插件时，不可编辑
                    this.atomDisabled = e.data.atomDisabled || false
                    // 当前插件在研发商店设置的环境变量
                    this.envConf = e.data.envConf || {}
                    this.hasInitData = true
                    console.log('Plugin initialized successfully! hasInitData:', this.hasInitData)
                    console.log('atomPropsValue:', this.atomPropsValue)
                    console.log('atomPropsModel:', this.atomPropsModel)
                } else {
                    console.warn('Received message but missing required data:', {
                        hasAtomPropsValue: !!(e.data && e.data.atomPropsValue),
                        hasAtomPropsModel: !!(e.data && e.data.atomPropsModel)
                    })
                }
            })
        },

        methods: {
            // 当iframe内容高度变化时，通知父级响应变化
            resize () {
                this.$nextTick(() => {
                    window.parent && window.parent.postMessage({ iframeHeight: document.body.scrollHeight }, '*')
                })
            }
        }
    }
</script>

<style lang="css">
    ::-webkit-scrollbar {
        width: 9px;
        height: 9px;
        background-color: white;
    }
    ::-webkit-scrollbar-thumb {
        height: 8px;
        border-radius: 20px;
        background-color: #a5a5a5;
    }
    .atom-disabled {
        pointer-events: none;
    }
</style>