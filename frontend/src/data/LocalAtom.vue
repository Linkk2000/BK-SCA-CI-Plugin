<template>
    <div id="local-atom" class="remote-atom" :class="{ 'atom-disabled': atomDisabled }">
        <Atom
            :atom-props-value="getAtomDefaultValue(taskJson && taskJson.input)"
            :atom-props-model="taskJson && taskJson.input"
            :atom-props-container-info="containerInfo"
            :atom-props-disabled="atomDisabled"
            :current-user-info="currentUserInfo"
            :env-conf="envConf"
        >
        </Atom>
    </div>
</template>

<script>
    import initTaskJson from './task.json'  // 该文件为调试专用，请注意task.json格式，若无内容，则task.json为{}
    import Atom from '../Atom'
    export default {
        name: 'local-atom',
        components: {
            Atom
        },
        computed: {
            taskJson () {
                return initTaskJson
            }
        },
        data () {
            return {
                // 插件所在的job的相关信息，如编译环境类型等
                containerInfo: {
                    baseOS: 'LINUX',
                    dispatchType: {
                        buildType: "DOCKER",
                        imageCode: "tlinux2_2",
                        imageName: "TLinux2.2公共镜像",
                        imageType: "BKSTORE",
                        imageVersion: "1.*",
                        value: "tlinux2_2"
                    }
                },
                // 当前访问用户的姓名、头像信息等
                currentUserInfo: {
                    userName: 'zhangsan',
                    chineseName: '张三'
                },
                // 当前插件是否可编辑，当在构建详情、预览、模板示例化出的流水线点开该插件时，不可编辑
                atomDisabled: false,
                envConf: {}
            }
        },
        methods: {
            getAtomDefaultValue (atomProps = {}) {
                return Object.keys(atomProps).reduce((formProps, key) => {
                    formProps[key] = atomProps[key].default
                    return formProps
                }, {})
            }
        }
    }
</script>

<style lang="css">
    #local-atom {
        width: 640px;
        margin: 30px auto;
        border: solid 1px #c4c6cc;
        padding: 32px;
    }
    .atom-disabled {
        pointer-events: none;
    }
</style>