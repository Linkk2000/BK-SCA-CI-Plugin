const assert = require('assert')
const fs = require('fs')
const path = require('path')
const vm = require('vm')
const { createRequire } = require('module')
const Vue = require('vue')
Vue.config.silent = true
const runtimeErrors = []
Vue.config.errorHandler = error => runtimeErrors.push(error)
const componentPath = path.resolve(__dirname, '../src/Atom.vue')
const taskData = require('../src/data/task.json')
const isScanner = /XMirror/.test(taskData.atomCode)
const isSca = taskData.atomCode === 'XMirrorSca'
const taskKey = isScanner ? (isSca ? 'scaTask' : 'sastTask') : 'easyopsTask'
const messages = []
const browser = {
    parent: { postMessage(message) { messages.push(JSON.parse(JSON.stringify(message))) } },
    addEventListener() {}
}
const quietConsole = { log() {}, warn() {}, error() {}, trace() {} }
const sdkPath = require.resolve('bkci-atom-components/package.json')
const sdkSource = fs.readFileSync(path.join(path.dirname(sdkPath), 'src/components/atomMixin.js'), 'utf8')
const sdkSandbox = { module: { exports: {} }, window: browser }
vm.runInNewContext(sdkSource.replace(/^import .*$/mg, '').replace('export default atomMixin', 'module.exports = atomMixin'), sdkSandbox)
const source = fs.readFileSync(componentPath, 'utf8')
const script = source.match(/<script>([\s\S]*?)<\/script>/)[1]
const sandbox = {
    module: { exports: {} }, require: createRequire(componentPath),
    atomMixin: sdkSandbox.module.exports, mockAjax() {}, taskData,
    window: browser, console: quietConsole, setTimeout, clearTimeout
}
vm.runInNewContext(script.replace(/^.*import .*$/mg, '').replace('export default {', 'module.exports = {'), sandbox)
const component = sandbox.module.exports
const tick = async () => { await Vue.nextTick(); await Vue.nextTick(); await Vue.nextTick() }
const lastValue = () => messages.filter(m => m.atomValue).slice(-1)[0].atomValue
const lastError = () => messages.filter(m => 'isError' in m).slice(-1)[0].isError

async function mount(values = {}, disabled = false) {
    messages.length = 0
    const instance = new Vue({
        ...component,
        propsData: { atomPropsValue: values, atomPropsModel: taskData.input, atomPropsDisabled: disabled },
        methods: { ...component.methods, fetchProjectList() {}, fetchAppList() {} },
        render(h) {
            return h('div', this.configurationInvalid ? [h('p', { attrs: { role: 'alert' } }, '配置不完整')] : [])
        }
    })
    instance.$bkMessage = () => {}
    instance.$mount()
    await tick()
    return instance
}
function validValues() {
    if (isScanner) return {
        server: 'https://scanner.example', token: 'test-token', projectId: 'project',
        projectName: 'Project', [isSca ? 'applicationId' : 'appId']: 'app',
        [isSca ? 'applicationName' : 'appName']: 'App'
    }
    if (taskData.atomCode === 'EasyOps-Flow') return {
        easyopsServer: '172.25.224.62', easyopsUsername: 'user', flowId: 'flow', flowInputs: ''
    }
    return {
        easyopsServer: '172.25.224.62', easyopsUsername: 'user', easyopsPackageId: 'package',
        environmentType: 'dev', repoFileName: 'app.zip', versionMode: 'auto', isUnzip: '0',
        stripFirst: '0', versionPrefix: 'ci-', conflictAction: '0', versionName: ''
    }
}
async function run() {
    assert(source.includes('v-if="configurationInvalid"') && source.includes('role="alert"'))
    let instance = await mount()
    assert.strictEqual(lastError(), true, 'new empty plugin reports error without interaction')
    assert.strictEqual(instance.configurationInvalid, true)
    assert(Object.values(instance.fieldErrors).some(e => e.show && e.message), 'missing fields have visible messages')
    if (taskData.atomCode === 'EasyOps-Registry') assert.strictEqual(instance.easyopsTask.isUnzip, '0', 'manifest default applied')
    Object.assign(instance[taskKey], validValues())
    await tick()
    assert.strictEqual(lastError(), false, 'fixing all fields clears platform error')
    assert.strictEqual(instance.configurationInvalid, false)
    assert.strictEqual(lastValue().easyopsUsername || lastValue().token, isScanner ? 'test-token' : 'user')

    const changedField = isScanner ? 'token' : 'easyopsUsername'
    instance[taskKey][changedField] = 'latest'
    // No tick or timer before Save: this is the original lost-value failure.
    if (isScanner) instance.saveConfiguration()
    else instance.handleSaveConfig()
    assert.strictEqual(lastValue()[changedField], 'latest', 'explicit save immediately sends final field value')
    instance.$set(instance.atomValue, 'extra', { nested: 'one' })
    await tick()
    instance.atomValue.extra.nested = 'two'
    await tick()
    assert.strictEqual(lastValue().extra.nested, 'two', 'deep atomValue changes are published')
    instance[taskKey][changedField] = ''
    await tick()
    assert.strictEqual(lastError(), true, 'clearing a required field reports error')
    instance.$destroy()

    const partial = validValues()
    partial[changedField] = ''
    instance = await mount(partial)
    assert.strictEqual(lastError(), true, 'opening incomplete historical configuration reports error')
    assert.strictEqual(instance.configurationInvalid, true)
    instance.$destroy()

    instance = await mount(validValues())
    assert.strictEqual(lastError(), false, 'opening valid configuration is not marked invalid')
    assert.strictEqual(instance.configurationInvalid, false)
    if (isScanner) {
        instance[taskKey].server = 'invalid-url'
        await tick()
        assert.strictEqual(lastError(), true, 'non-empty invalid URL is rejected')
        assert(instance.fieldErrors.server.message)
    } else if (taskData.atomCode === 'EasyOps-Flow') {
        instance.easyopsTask.flowInputs = '{'
        await tick()
        assert.strictEqual(lastError(), true, 'invalid optional JSON is reported')
        instance.easyopsTask.flowInputs = '{}'
        await tick()
        assert.strictEqual(lastError(), false)
    } else {
        instance.easyopsTask.versionMode = 'custom'
        instance.easyopsTask.versionName = ''
        await tick()
        assert.strictEqual(lastError(), true, 'conditional required version name is checked')
        instance.easyopsTask.versionMode = 'auto'
        await tick()
        assert.strictEqual(lastError(), false)
    }
    instance.$destroy()

    const readOnlyValues = {}
    instance = await mount(readOnlyValues, true)
    assert.deepStrictEqual(readOnlyValues, {}, 'read-only initialization does not change supplied values')
    assert.strictEqual(messages.length, 0, 'read-only mode does not send editable values/errors')
    instance.$destroy()
    assert.strictEqual(messages.length, 0)
    assert.deepStrictEqual(runtimeErrors, [], 'Vue lifecycle and watchers have no errors')
    console.log('PASS: real Vue initialization, deep watchers, immediate save, validation, defaults and read-only mode')
}
run().catch(error => { console.error(error); process.exitCode = 1 })

