<template>
    <section class="bk-form bk-form-vertical atom-form">
        <!-- SAST服务器地址 -->
        <div class="form-group" :class="{ 'has-error': errors.server.show }">
            <label class="form-label required">SAST服务器地址</label>
            <input 
                type="text" 
                class="form-input"
                v-model="formData.server"
                @blur="handleBlur('server')"
                @input="handleInput('server')"
                placeholder="请输入SAST服务器地址"
                :disabled="atomPropsDisabled"
            />
            <div class="error-message" v-if="errors.server.show">
                {{ errors.server.message }}
            </div>
        </div>

        <!-- Token -->
        <div class="form-group" :class="{ 'has-error': errors.token.show }">
            <label class="form-label required">Token</label>
            <input 
                type="password" 
                class="form-input"
                v-model="formData.token"
                @blur="handleBlur('token')"
                @input="handleInput('token')"
                placeholder="请输入Token"
                :disabled="atomPropsDisabled"
            />
            <div class="error-message" v-if="errors.token.show">
                {{ errors.token.message }}
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
            
            <!-- 成功提示 -->
            <transition name="fade">
                <div class="test-result success" v-if="testResult.show && testResult.type === 'success'">
                    <span class="icon">✓</span>
                    <span class="message">连接成功！</span>
                </div>
            </transition>
            
            <!-- 失败提示 -->
            <transition name="fade">
                <div class="test-result error" v-if="testResult.show && testResult.type === 'error'">
                    <span class="icon">✗</span>
                    <span class="message">{{ testResult.message }}</span>
                </div>
            </transition>
        </div>
    </section>
</template>

<script>
    // 需引用atomMixin
    import { atomMixin } from 'bkci-atom-components'

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
                formData: {
                    server: '',
                    token: ''
                },
                errors: {
                    server: {
                        show: false,
                        message: ''
                    },
                    token: {
                        show: false,
                        message: ''
                    }
                },
                touched: {
                    server: false,
                    token: false
                },
                isLoading: false,
                testResult: {
                    show: false,
                    type: '', // 'success' or 'error'
                    message: ''
                },
                successTimer: null
            }
        },
        mounted() {
            // 从 atomValue 中初始化表单数据
            if (this.atomValue) {
                this.formData.server = this.atomValue.server || ''
                this.formData.token = this.atomValue.token || ''
            }
        },
        watch: {
            'formData.server'(newVal) {
                this.atomValue.server = newVal
                this.clearTestResult()
            },
            'formData.token'(newVal) {
                this.atomValue.token = newVal
                this.clearTestResult()
            }
        },
        methods: {
            // 验证单个字段
            validateField(fieldName) {
                const value = this.formData[fieldName]
                
                if (!value || value.trim() === '') {
                    this.errors[fieldName] = {
                        show: true,
                        message: '字段不能为空'
                    }
                    return false
                } else {
                    this.errors[fieldName] = {
                        show: false,
                        message: ''
                    }
                    return true
                }
            },
            
            // 失焦验证
            handleBlur(fieldName) {
                this.touched[fieldName] = true
                this.validateField(fieldName)
            },
            
            // 输入时验证
            handleInput(fieldName) {
                if (this.touched[fieldName]) {
                    this.validateField(fieldName)
                }
            },
            
            // 验证所有字段
            validateAll() {
                let isValid = true
                Object.keys(this.formData).forEach(key => {
                    if (!this.validateField(key)) {
                        isValid = false
                    }
                })
                return isValid
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
            
            // 测试连接
            async testConnection() {
                // 验证所有必填字段
                if (!this.validateAll()) {
                    return
                }
                
                this.isLoading = true
                this.clearTestResult()
                
                try {
                    // 这里调用实际的API接口测试连接
                    // 示例：使用axios发送请求
                    const response = await this.$ajax({
                        url: `${this.formData.server}/api/test-connection`,
                        method: 'POST',
                        headers: {
                            'Authorization': `Bearer ${this.formData.token}`
                        },
                        timeout: 10000 // 10秒超时
                    })
                    
                    // 连接成功
                    this.testResult = {
                        show: true,
                        type: 'success',
                        message: '连接成功！'
                    }
                    
                    // 3秒后自动隐藏成功提示
                    this.successTimer = setTimeout(() => {
                        this.clearTestResult()
                    }, 3000)
                    
                } catch (error) {
                    // 连接失败
                    let errorMessage = '连接失败，请检查服务器地址和Token是否正确'
                    
                    if (error.message) {
                        errorMessage = error.message
                    } else if (error.response && error.response.data && error.response.data.message) {
                        errorMessage = error.response.data.message
                    }
                    
                    this.testResult = {
                        show: true,
                        type: 'error',
                        message: errorMessage
                    }
                } finally {
                    this.isLoading = false
                }
            },
            
            // 当用户输入相关参数后，把字段写入到this.atomValue
            handleUpdate(name, value) {
                this.atomValue[name] = value
            }
        },
        beforeDestroy() {
            if (this.successTimer) {
                clearTimeout(this.successTimer)
            }
        }
    }
</script>

<style lang="scss" scoped>
    .atom-form {
        padding: 20px 0;
        
        .form-group {
            margin-bottom: 20px;
            
            &.has-error {
                .form-input {
                    border-color: #ff5656;
                    
                    &:focus {
                        border-color: #ff5656;
                        box-shadow: 0 0 0 2px rgba(255, 86, 86, 0.1);
                    }
                }
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
        
        .form-input {
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
                
                .icon {
                    font-size: 16px;
                    font-weight: bold;
                }
                
                &.success {
                    color: #2dcb56;
                    
                    .icon {
                        color: #2dcb56;
                    }
                }
                
                &.error {
                    color: #ff5656;
                    
                    .icon {
                        color: #ff5656;
                    }
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
