<template>
    <div id="local-atom" class="remote-atom" :class="{ 'atom-disabled': atomDisabled }">
        <Atom
            v-if="taskJson && taskJson.input"
            :atom-props-value="getAtomDefaultValue(taskJson.input)"
            :atom-props-model="taskJson.input"
            :atom-props-container-info="containerInfo"
            :atom-props-disabled="atomDisabled"
            :current-user-info="currentUserInfo"
            :env-conf="envConf"
        >
        </Atom>
        <div v-else style="padding: 20px; color: red;">
            错误：无法加载 task.json 数据，请检查 frontend/src/data/task.json 是否存在且格式正确。
        </div>
    </div>
</template>

<script>
    import initTaskJson from './task.json'
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
        created() {
            console.warn('[LocalAtom] Component initialized')
        },
        data () {
            return {
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
                currentUserInfo: {
                    userName: 'zhangsan',
                    chineseName: '张三'
                },
                atomDisabled: false,
                envConf: {}
            }
        },
        methods: {
            getAtomDefaultValue (atomProps = {}) {
                return Object.keys(atomProps).reduce((formProps, key) => {
                    formProps[key] = atomProps[key].default || ''
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
        background: #fff;
    }
    .atom-disabled {
        pointer-events: none;
    }
</style>
