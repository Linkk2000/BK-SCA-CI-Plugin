<template>
    <section class="bk-form bk-form-vertical atom-form">
        <!-- 第一部分：服务器信息 -->
        <div class="form-section">
            <div class="section-header">
                <h3 class="section-title">服务器信息</h3>
                <a
                    v-if="wikiUrl"
                    class="wiki-link"
                    :href="wikiUrl"
                    target="_blank"
                    rel="noopener noreferrer"
                    title="在新窗口打开插件使用文档"
                >使用文档 ↗</a>
            </div>
            
            <!-- SCA服务器地址 -->
            <div class="form-group" :class="{ 'has-error': fieldErrors.server.show }">
                <label class="form-label required">SCA服务器地址</label>
                <input 
                    type="text" 
                    class="form-input"
                    v-model="scaTask.server"
                    @blur="handleBlur('server')"
                    @input="updateTaskField('server', $event.target.value)"
                    placeholder="请输入SCA服务器地址"
                    :disabled="atomPropsDisabled"
                />
                <div class="error-message" v-if="fieldErrors.server.show">
                    {{ fieldErrors.server.message }}
                </div>
            </div>

            <!-- Token -->
            <div class="form-group" :class="{ 'has-error': fieldErrors.token.show }">
                <label class="form-label required">Token</label>
                <input 
                    type="password" 
                    class="form-input"
                    v-model="scaTask.token"
                    @blur="handleBlur('token')"
                    @input="updateTaskField('token', $event.target.value)"
                    placeholder="请输入Token"
                    :disabled="atomPropsDisabled"
                />
                <div class="error-message" v-if="fieldErrors.token.show">
                    {{ fieldErrors.token.message }}
                </div>
            </div>

            <!-- 测试连接按钮 -->
            <div class="test-connection-wrapper">
                <button 
                    class="test-btn"
                    :class="{ 'is-loading': isLoading }"
                    @click="testConnection"
                    :disabled="atomPropsDisabled || isLoading"
                >
                    <span v-if="isLoading">测试中...</span>
                    <span v-else>测试连接</span>
                </button>
                
                <transition name="fade">
                    <div class="test-result success" v-if="testResult.show && testResult.type === 'success'">
                        <span class="message">连接成功</span>
                    </div>
                </transition>
                
                <transition name="fade">
                    <div class="test-result error" v-if="testResult.show && testResult.type === 'error'">
                        <span class="message">{{ testResult.message }}</span>
                    </div>
                </transition>
            </div>
        </div>

        <!-- 第二部分：扫描任务信息 -->
        <div class="form-section">
            <h3 class="section-title">扫描任务信息</h3>
            
            <!-- 项目选择 -->
            <div class="form-group" :class="{ 'has-error': fieldErrors.projectName.show }">
                <label class="form-label required">项目</label>
                <bk-select 
                    v-model="scaTask.projectId"
                    :searchable="true"
                    :loading="projectLoading"
                    :disabled="atomPropsDisabled || !scaTask.server || !scaTask.token"
                    placeholder="请选择或搜索项目"
                    @change="handleProjectChange"
                    @clear="resetProjectData"
                    @toggle="handleProjectToggle"
                >
                    <bk-option 
                        v-for="project in projectList" 
                        :key="project.id" 
                        :id="project.id" 
                        :name="project.name"
                    >
                    </bk-option>
                </bk-select>
                <div class="error-message" v-if="fieldErrors.projectName.show">
                    {{ fieldErrors.projectName.message }}
                </div>
                <div class="field-tip" v-if="!scaTask.server || !scaTask.token">
                    请先完成服务器信息配置并测试连接
                </div>
            </div>

            <!-- 应用选择 -->
            <div class="form-group" :class="{ 'has-error': fieldErrors.applicationName.show }">
                <label class="form-label required">应用</label>
                <bk-select 
                    v-model="scaTask.applicationId"
                    :searchable="true"
                    :loading="appLoading"
                    :disabled="atomPropsDisabled || !scaTask.projectId"
                    placeholder="请选择或搜索应用"
                    @change="handleAppChange"
                    @clear="resetAppData"
                    @toggle="handleAppToggle"
                >
                    <bk-option 
                        v-for="app in appList" 
                        :key="app.id" 
                        :id="app.id" 
                        :name="app.label || app.name"
                    >
                    </bk-option>
                </bk-select>
                <div class="error-message" v-if="fieldErrors.applicationName.show">
                    {{ fieldErrors.applicationName.message }}
                </div>
                <div class="field-tip" v-if="!scaTask.projectId">
                    请先选择关联项目
                </div>
            </div>
        </div>

        <!-- 底部操作栏 -->
        <div class="form-actions">
            <button 
                class="save-btn" 
                @click="saveConfiguration"
                :disabled="atomPropsDisabled"
            >
                保存配置
            </button>
            <transition name="fade">
                <div class="save-status" v-if="saveStatus.show">
                    {{ saveStatus.message }}
                </div>
            </transition>
        </div>
    </section>
</template>

<script>
    // 需引用atomMixin
    import { atomMixin } from 'bkci-atom-components'
    import { mockAjax } from '@/utils/mock'

    // 插件使用文档地址由 webpack DefinePlugin 在编译时注入（环境变量 WIKI_URL），未配置则为空
    const wikiUrl = typeof WIKI_URL === 'string' ? WIKI_URL : ''

    export default {
        name: 'atom',
        mixins: [atomMixin],    // 需引用atomMixin
        props: {
            atomPropsContainerInfo: {
                type: Object,
                default: () => ({})
            },
            atomPropsDisabled: {
                type: Boolean,
                default: false
            },
            currentUserInfo: {
                type: Object,
                default: () => ({})
            },
            envConf: {
                type: Object,
                default: () => ({})
            }
        },
        data() {
            return {
                wikiUrl,
                // 核心：本地数据副本，所有 UI 绑定均基于此
                scaTask: {
                    server: '',
                    token: '',
                    projectId: '',
                    projectName: '',
                    applicationId: '',
                    applicationName: ''
                },
                // 校验状态保持独立
                fieldErrors: {
                    server: { show: false, message: '' },
                    token: { show: false, message: '' },
                    projectName: { show: false, message: '' },
                    applicationName: { show: false, message: '' }
                },
                touched: {
                    server: false,
                    token: false,
                    projectName: false,
                    applicationName: false
                },
                isLoading: false,
                testResult: { show: false, type: '', message: '' },
                saveStatus: { show: false, message: '' },
                successTimer: null,
                projectList: [],
                projectLoading: false,
                projectSearchKeyword: '',
                appList: [],
                appLoading: false,
                appSearchKeyword: ''
            }
        },
        computed: {
            isLocalDev() {
                // 读取 main.js 中设置的环境标识
                return window.__ATOM_ENV__ && window.__ATOM_ENV__.isLocal === true
            },
            useMock() {
                return this.isLocalDev
            },
            ajax() {
                return this.useMock ? mockAjax : this.$ajax
            }
        },
        mounted() {
            console.log('Atom component mounted')
            this.initScaTask()
            
            // 暴露组件实例到全局，方便调试
            window.__ATOM_INSTANCE__ = this
            console.log('%c[调试提示] 组件实例已暴露到 window.__ATOM_INSTANCE__', 'color: #2dcb56; font-weight: bold;')
            console.log('可在控制台使用: __ATOM_INSTANCE__.scaTask 或 __ATOM_INSTANCE__.atomValue')
        },
        methods: {
            // 统一规范化 server 输入，避免出现 http://https//xxx 这类错误 URL
            // 约束：必须显式以 http:// 或 https:// 开头；否则判定为不合法（不做任何自动修复/补全）
            normalizeServerUrl(rawServer) {
                // 打印url
                console.log('url', rawServer)
                let server = (rawServer || '').trim()
                if (!server) return ''

                // 不使用正则，严格要求协议前缀
                const hasValidPrefix = server.startsWith('http://') || server.startsWith('https://')
                if (!hasValidPrefix) return ''
                // 说明url合格
                console.log("Url is effective.")
                // 去掉末尾 /
                if (server.endsWith('/')) server = server.substring(0, server.length - 1)
                return server
            },

            // 封装一：初始化逻辑
            initScaTask() {
                console.log('%c[初始化] 开始初始化 scaTask', 'color: #ff9800; font-weight: bold;')
                console.log('[初始化] atomValue 原始数据:', JSON.parse(JSON.stringify(this.atomValue)))
                
                if (this.atomValue) {
                    // 深拷贝平台数据到本地副本
                    const platformData = JSON.parse(JSON.stringify(this.atomValue))
                    Object.keys(this.scaTask).forEach(key => {
                        this.scaTask[key] = platformData[key] || ''
                    })
                    
                    console.log('[初始化] 拷贝后的 scaTask:', JSON.parse(JSON.stringify(this.scaTask)))

                    // 初始化回显列表
                    if (this.scaTask.server && this.scaTask.token) {
                        console.log('[初始化] 准备获取项目列表...')
                        this.fetchProjectList()
                        if (this.scaTask.projectId) {
                            console.log('[初始化] 准备获取应用列表...')
                            this.fetchAppList()
                        }
                    }
                }
                
                console.log('%c[初始化] 完成', 'color: #ff9800; font-weight: bold;')
            },

            // 封装二：统一字段更新入口（替代 watch）
            updateTaskField(field, value) {
                this.scaTask[field] = value
                
                // 显式联动逻辑
                if (field === 'server' || field === 'token') {
                    this.clearTestResult()
                    this.resetProjectData() 
                } else if (field === 'projectId') {
                    this.resetAppData()
                }

                if (this.touched[field]) {
                    this.validateField(field)
                }
            },

            // 封装三：同步回平台
            syncToPlatform() {
                Object.keys(this.scaTask).forEach(key => {
                    this.$set(this.atomValue, key, this.scaTask[key])
                })
            },

            // 重置项目及以下所有数据
            resetProjectData() {
                console.warn('%c[重置] resetProjectData() 被调用', 'color: #f44336; font-weight: bold;')
                console.trace('[重置] 调用堆栈')
                this.scaTask.projectId = ''
                this.scaTask.projectName = ''
                this.projectList = []
                this.resetAppData()
            },
            
            // 重置应用数据
            resetAppData() {
                console.warn('%c[重置] resetAppData() 被调用', 'color: #f44336; font-weight: bold;')
                console.trace('[重置] 调用堆栈')
                this.scaTask.applicationId = ''
                this.scaTask.applicationName = ''
                this.appList = []
            },
            
            // 验证单个字段
            validateField(fieldName) {
                const value = this.scaTask[fieldName]

                if (fieldName === 'server') {
                    const server = (value || '').trim()
                    if (!server) {
                        this.fieldErrors.server = { show: true, message: '字段不能为空' }
                        return false
                    }
                    const isValid = server.startsWith('http://') || server.startsWith('https://')
                    if (!isValid) {
                        this.fieldErrors.server = { show: true, message: '服务器地址必须以 http:// 或 https:// 开头' }
                        return false
                    }
                    this.fieldErrors.server = { show: false, message: '' }
                    return true
                }
                
                if (fieldName === 'projectName') {
                    if (!this.scaTask.projectId) {
                        this.fieldErrors.projectName = { show: true, message: '请选择项目' }
                        return false
                    }
                    this.fieldErrors.projectName = { show: false, message: '' }
                    return true
                }
                
                if (fieldName === 'applicationName') {
                    if (!this.scaTask.applicationId) {
                        this.fieldErrors.applicationName = { show: true, message: '请选择应用' }
                        return false
                    }
                    this.fieldErrors.applicationName = { show: false, message: '' }
                    return true
                }
                
                if (!value || (typeof value === 'string' && value.trim() === '')) {
                    this.fieldErrors[fieldName] = { show: true, message: '字段不能为空' }
                    return false
                }
                this.fieldErrors[fieldName] = { show: false, message: '' }
                return true
            },
            
            // 失焦验证
            handleBlur(fieldName) {
                this.touched[fieldName] = true
                this.validateField(fieldName)
            },
            
            // 验证所有字段
            validateAll(showErrors = true) {
                let isValid = true
                // 仅验证我们需要同步到平台的字段
                const fieldsToValidate = ['server', 'token', 'projectName', 'applicationName']
                fieldsToValidate.forEach(key => {
                    const fieldValid = this.checkFieldValid(key)
                    if (!fieldValid) {
                        isValid = false
                    }
                    if (showErrors && this.fieldErrors[key]) {
                        this.fieldErrors[key].show = !fieldValid
                    }
                })
                return isValid
            },

            // 内部纯校验逻辑
            checkFieldValid(fieldName) {
                const value = this.scaTask[fieldName]
                if (fieldName === 'projectName') return !!this.scaTask.projectId
                if (fieldName === 'applicationName') return !!this.scaTask.applicationId
                return !!(value && typeof value === 'string' && value.trim() !== '')
            },
            
            // 清除测试结果
            clearTestResult() {
                this.testResult.show = false
                this.testResult.type = ''
                this.testResult.message = ''
                if (this.successTimer) {
                    clearTimeout(this.successTimer)
                    this.successTimer = null
                }
            },
            
            // 测试连接 - 使用项目列表接口测试（不带参数获取全量）
            async testConnection() {
                const isServerValid = this.validateField('server')
                const isTokenValid = this.validateField('token')
                
                if (!isServerValid || !isTokenValid) return
                
                this.isLoading = true
                this.clearTestResult()
                
                try {
                    const rawServer = this.scaTask.server
                    const server = this.normalizeServerUrl(rawServer)
                    if (!server) throw new Error('SCA服务器地址不能为空')
                    
                    const apiPath = '/sca/api-v1/project/list'
                    const token = this.scaTask.token.trim()
                    const url = `${server}${apiPath}`

                    console.warn('[SCA][testConnection] rawServer=', rawServer)
                    console.warn('[SCA][testConnection] normalizedServer=', server)
                    console.warn('[SCA][testConnection] url=', url)
                    
                    const response = await this.ajax({
                        url: url,
                        method: 'POST',
                        headers: {
                            'OpenApiUserToken': token,
                            'Content-Type': 'application/json'
                        },
                        data: {
                            pageNum: 1,
                            pageSize: 1
                        },
                        timeout: 10000
                    })
                    
                    if (response && response.code === 0) {
                        this.testResult = { show: true, type: 'success', message: '连接成功！' }
                        this.successTimer = setTimeout(() => this.clearTestResult(), 3000)
                    } else {
                        throw new Error((response && response.message) || '连接测试失败：响应异常')
                    }
                } catch (error) {
                    this.testResult = { show: true, type: 'error', message: error.message || '连接失败' }
                } finally {
                    this.isLoading = false
                }
            },
            
            // 获取项目列表
            async fetchProjectList(keyword = '') {
                console.log('%c[获取项目] fetchProjectList() 开始', 'color: #2196f3; font-weight: bold;')
                console.log('[获取项目] 当前 scaTask.projectId=', this.scaTask.projectId)
                console.log('[获取项目] 当前 scaTask.applicationId=', this.scaTask.applicationId)
                
                const isServerValid = this.validateField('server')
                const isTokenValid = this.validateField('token')
                if (!isServerValid || !isTokenValid) {
                    console.warn('[获取项目] 校验失败，退出')
                    return
                }
                
                this.projectLoading = true
                try {
                    const rawServer = this.scaTask.server
                    const server = this.normalizeServerUrl(rawServer)
                    if (!server) return
                    const url = `${server}/sca/api-v1/project/list`

                    console.warn('[SCA][fetchProjectList] rawServer=', rawServer)
                    console.warn('[SCA][fetchProjectList] normalizedServer=', server)
                    console.warn('[SCA][fetchProjectList] url=', url)
                    
                    const requestData = {
                        pageNum: 1,
                        pageSize: 20
                    }
                    if (keyword) {
                        requestData.nameOrDescription = keyword
                    }
                    
                    const response = await this.ajax({
                        url: url,
                        method: 'POST',
                        headers: { 
                            'OpenApiUserToken': this.scaTask.token.trim(),
                            'Content-Type': 'application/json'
                        },
                        data: requestData
                    })
                    
                    console.log('[获取项目] 完整响应:', response)
                    console.log('[获取项目] response.code=', response && response.code)
                    console.log('[获取项目] response.data=', response && response.data)
                    console.log('[获取项目] response.data.records=', response && response.data && response.data.records)
                    
                    if (response && response.code === 0 && response.data && response.data.records) {
                        this.projectList = response.data.records.map(item => ({
                            id: item.id,
                            name: item.name
                        }))
                        console.log('[获取项目] 映射后的 projectList:', this.projectList)
                        console.log('[获取项目] 成功获取项目列表，数量:', this.projectList.length)
                        console.log('[获取项目] 完成后 scaTask.projectId=', this.scaTask.projectId)
                        console.log('[获取项目] 完成后 scaTask.applicationId=', this.scaTask.applicationId)
                    } else {
                        console.warn('[获取项目] 响应数据结构不符合预期')
                    }
                } catch (error) {
                    console.error('[获取项目] 失败:', error)
                } finally {
                    this.projectLoading = false
                }
            },
            
            // 获取应用列表
            async fetchAppList(keyword = '') {
                console.log('%c[获取应用] fetchAppList() 开始', 'color: #2196f3; font-weight: bold;')
                console.log('[获取应用] 当前 scaTask.projectId=', this.scaTask.projectId)
                console.log('[获取应用] 当前 scaTask.applicationId=', this.scaTask.applicationId)
                
                const isServerValid = this.validateField('server')
                const isTokenValid = this.validateField('token')
                if (!isServerValid || !isTokenValid || !this.scaTask.projectId) {
                    console.warn('[获取应用] 校验失败或 projectId 为空，退出')
                    return
                }
                
                this.appLoading = true
                try {
                    const rawServer = this.scaTask.server
                    const server = this.normalizeServerUrl(rawServer)
                    if (!server) return
                    const url = `${server}/sca/api-v1/git/list`

                    console.warn('[SCA][fetchAppList] rawServer=', rawServer)
                    console.warn('[SCA][fetchAppList] normalizedServer=', server)
                    console.warn('[SCA][fetchAppList] url=', url)
                    
                    const requestData = {
                        pageNum: 1,
                        pageSize: 20,
                        projectIdList: [this.scaTask.projectId]
                    }
                    if (keyword) {
                        requestData.words = keyword
                    }
                    
                    const response = await this.ajax({
                        url: url,
                        method: 'POST',
                        headers: { 
                            'OpenApiUserToken': this.scaTask.token.trim(),
                            'Content-Type': 'application/json'
                        },
                        data: requestData
                    })
                    
                    if (response && response.code === 0 && response.data && response.data.records) {
                        // git/list 的记录自带 version（分支）。同名应用只有分支不同，
                        // 下拉里必须把分支显示出来；name 保持纯应用名，回填给隐藏字段时不带分支
                        this.appList = response.data.records.map(item => ({
                            id: item.applicationId,
                            name: item.applicationName,
                            label: item.version
                                ? `${item.applicationName}  [${item.version}]`
                                : item.applicationName
                        }))
                        console.log('[获取应用] 成功获取应用列表，数量:', this.appList.length)
                        console.log('[获取应用] 完成后 scaTask.applicationId=', this.scaTask.applicationId)
                    }
                } catch (error) {
                    console.error('[获取应用] 失败:', error)
                } finally {
                    this.appLoading = false
                }
            },
            
            // 项目选择变化
            handleProjectChange(projectId) {
                console.log('%c[事件] handleProjectChange 被触发', 'color: #9c27b0; font-weight: bold;')
                console.log('[事件] projectId=', projectId)
                console.log('[事件] projectList.length=', this.projectList.length)
                
                const project = this.projectList.find(p => p.id === projectId)
                if (project) {
                    console.log('[事件] 找到项目:', project.name)
                    this.updateTaskField('projectId', project.id)
                    this.updateTaskField('projectName', project.name)
                    this.fetchAppList()
                } else {
                    // 🔥 修复：只有在列表不为空时才重置（避免初始化时因列表未加载而误清空）
                    if (this.projectList.length > 0) {
                        console.warn('[事件] 项目列表不为空但未找到匹配项，重置数据')
                        this.resetProjectData()
                    } else {
                        console.log('[事件] 项目列表为空，跳过重置（可能是初始化中）')
                    }
                }
            },
            
            // 应用选择变化
            handleAppChange(applicationId) {
                console.log('%c[事件] handleAppChange 被触发', 'color: #9c27b0; font-weight: bold;')
                console.log('[事件] applicationId=', applicationId)
                console.log('[事件] appList.length=', this.appList.length)
                
                const app = this.appList.find(a => a.id === applicationId)
                if (app) {
                    console.log('[事件] 找到应用:', app.name)
                    this.updateTaskField('applicationId', app.id)
                    this.updateTaskField('applicationName', app.name)
                } else {
                    // 🔥 修复：只有在列表不为空时才重置（避免初始化时因列表未加载而误清空）
                    if (this.appList.length > 0) {
                        console.warn('[事件] 应用列表不为空但未找到匹配项，重置数据')
                        this.resetAppData()
                    } else {
                        console.log('[事件] 应用列表为空，跳过重置（可能是初始化中）')
                    }
                }
            },

            handleProjectToggle(isOpen) {
                if (isOpen && this.projectList.length === 0) this.fetchProjectList()
                if (!isOpen) {
                    this.touched.projectName = true
                    this.validateField('projectName')
                }
            },

            handleAppToggle(isOpen) {
                if (isOpen && this.appList.length === 0 && this.scaTask.projectId) this.fetchAppList()
                if (!isOpen) {
                    this.touched.applicationName = true
                    this.validateField('applicationName')
                }
            },
            
            // 项目搜索
            handleProjectSearch(keyword) {
                this.projectSearchKeyword = keyword
                this.fetchProjectList(keyword)
            },
            
            // 应用搜索
            handleAppSearch(keyword) {
                this.appSearchKeyword = keyword
                this.fetchAppList(keyword)
            },

            // 保存配置
            saveConfiguration() {
                const isValid = this.validateAll()
                if (isValid) {
                    this.syncToPlatform() // 显式同步到 atomValue
                    this.setAtomIsError(false)
                    this.saveStatus = { show: true, message: '保存成功' }
                } else {
                    this.setAtomIsError(true)
                    this.saveStatus = { show: true, message: '请完善必填信息' }
                }
                setTimeout(() => this.saveStatus.show = false, 3000)
            }
        },
        beforeDestroy() {
            if (this.successTimer) {
                clearTimeout(this.successTimer)
            }
            
            // 清理全局引用
            if (window.__ATOM_INSTANCE__ === this) {
                window.__ATOM_INSTANCE__ = null
            }
            
            // 💡 只有在点击保存按钮时才同步到 atomValue
            // 侧边栏关闭时仅回传当前的校验状态，不强制覆盖数据，保护已保存的数据不被中间态破坏
            const isFinalValid = this.validateAll(false)
            this.setAtomIsError(!isFinalValid)
            
            console.log('[BKCI-ATOM] Cleanup completed. Valid:', isFinalValid)
        }
    }
</script>

<style lang="scss" scoped>
    .atom-form {
        padding: 10px 0;

        .form-section {
            background: #fff;
            border: 1px solid #dcdee5;
            border-radius: 2px;
            padding: 20px;
            margin-bottom: 20px;

            .section-title {
                margin: 0 0 20px 0;
                padding-bottom: 10px;
                border-bottom: 1px solid #f0f1f5;
                font-size: 16px;
                color: #313238;
                font-weight: bold;
            }

            .section-header {
                display: flex;
                justify-content: space-between;
                align-items: center;
                margin: 0 0 20px 0;
                padding-bottom: 10px;
                border-bottom: 1px solid #f0f1f5;

                .section-title {
                    margin: 0;
                    padding: 0;
                    border: none;
                }

                .wiki-link {
                    font-size: 14px;
                    color: #3a84ff;
                    text-decoration: none;

                    &:hover {
                        color: #4e94ff;
                        text-decoration: underline;
                    }
                }
            }
        }
        
        .form-group {
            margin-bottom: 20px;
            
            &:last-child {
                margin-bottom: 0;
            }
            
            &.has-error {
                .form-input,
                .bk-select {
                    border-color: #ff5656 !important;
                    
                    &:focus,
                    &.is-focus {
                        border-color: #ff5656 !important;
                        box-shadow: 0 0 0 2px rgba(255, 86, 86, 0.1);
                    }
                }
            }
        }

        .form-actions {
            margin-top: 30px;
            padding: 20px 0;
            border-top: 1px solid #f0f1f5;
            display: flex;
            align-items: center;
            gap: 15px;

            .save-btn {
                height: 40px;
                padding: 0 40px;
                font-size: 14px;
                color: #fff;
                background-color: #3a84ff;
                border: none;
                border-radius: 2px;
                cursor: pointer;
                font-weight: bold;
                transition: background-color 0.2s;
                
                &:hover:not(:disabled) {
                    background-color: #4e94ff;
                }
                
                &:disabled {
                    background-color: #dcdee5;
                    cursor: not-allowed;
                }
            }

            .save-status {
                font-size: 14px;
                color: #63656e;
                animation: fadeIn 0.3s ease-in;
            }
        }
        
        .form-label {
            display: block;
            margin-bottom: 8px;
            font-size: 14px;
            color: #63656e;
            font-weight: 500;
            
            &.required::before {
                content: '*';
                color: #ff5656;
                margin-right: 4px;
            }
        }
        
        .form-input,
        .form-select {
            width: 100%;
            height: 36px;
            padding: 0 12px;
            font-size: 14px;
            line-height: 36px;
            color: #63656e;
            background-color: #fff;
            border: 1px solid #c4c6cc;
            border-radius: 2px;
            outline: none;
            transition: border-color 0.2s, box-shadow 0.2s;
            
            &:hover {
                border-color: #979ba5;
            }
            
            &:focus {
                border-color: #3a84ff;
                box-shadow: 0 0 0 2px rgba(58, 132, 255, 0.1);
            }
            
            &:disabled {
                background-color: #fafbfd;
                color: #c4c6cc;
                cursor: not-allowed;
            }
            
            &::placeholder {
                color: #c4c6cc;
            }
        }
        
        .form-select {
            cursor: pointer;
            padding-right: 30px;
            background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='12' viewBox='0 0 12 12'%3E%3Cpath fill='%2363656e' d='M6 9L1 4h10z'/%3E%3C/svg%3E");
            background-repeat: no-repeat;
            background-position: right 10px center;
            background-size: 12px;
            appearance: none;
            
            &:disabled {
                background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='12' viewBox='0 0 12 12'%3E%3Cpath fill='%23c4c6cc' d='M6 9L1 4h10z'/%3E%3C/svg%3E");
            }
        }
        
        .field-tip {
            margin-top: 6px;
            font-size: 12px;
            color: #979ba5;
            line-height: 1.5;
        }
        
        .error-message {
            margin-top: 6px;
            font-size: 12px;
            color: #ff5656;
            line-height: 1.5;
        }
        
        .test-connection-wrapper {
            margin-top: 24px;
            display: flex;
            align-items: center;
            gap: 12px;
            
            .test-btn {
                height: 36px;
                padding: 0 24px;
                font-size: 14px;
                color: #fff;
                background-color: #3a84ff;
                border: none;
                border-radius: 2px;
                cursor: pointer;
                outline: none;
                transition: background-color 0.2s;
                
                &:hover:not(:disabled) {
                    background-color: #4e94ff;
                }
                
                &:active:not(:disabled) {
                    background-color: #2c6be6;
                }
                
                &:disabled {
                    background-color: #dcdee5;
                    cursor: not-allowed;
                }
                
                &.is-loading {
                    background-color: #699df4;
                }
            }
            
            .test-result {
                display: flex;
                align-items: center;
                gap: 6px;
                font-size: 14px;
                animation: fadeIn 0.3s ease-in;
                
                &.success {
                    color: #2dcb56;
                }
                
                &.error {
                    color: #ff5656;
                }
            }
        }
    }
    
    // 淡入淡出动画
    .fade-enter-active, .fade-leave-active {
        transition: opacity 0.3s ease;
    }
    
    .fade-enter, .fade-leave-to {
        opacity: 0;
    }
    
    @keyframes fadeIn {
        from {
            opacity: 0;
            transform: translateX(-10px);
        }
        to {
            opacity: 1;
            transform: translateX(0);
        }
    }
</style>
