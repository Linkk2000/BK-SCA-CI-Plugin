# 插件配置同步与初始校验

对应 Issue：#2。

配置初始化完成后立即校验，未配置完整时显示顶部提示和字段错误，通过 `isError` 通知平台；补齐配置后清除错误。深度监听业务表单和 `atomValue`，主动调用 SDK 的 `setAtomValue()`。Vue 合并同一轮字段更新，不增加 1 秒防抖；显式保存和组件销毁前立即发送。

SAST/SCA 编辑内容现在持续同步到平台内存中的插件配置，包括未填完整的草稿；插件按钮的校验不能替代流水线最终保存。只读初始化不修改平台传入对象，不主动上报可编辑状态。

## 验证

- `cd frontend && npm ci && npm run test:configuration`：使用真实 Vue 响应式与 SDK 消息方法，覆盖空配置、历史缺失配置、完整配置、编辑后同轮保存、深度变化、修正/清空字段、只读模式及相应条件校验。
- `npm run test:ui-contract --if-present`：原有测试保持通过。
- `npm run public`：生产构建通过，已有包体积提示保留。
- `python3 -m http.server 8765 --bind 127.0.0.1`（在仓库根目录运行），打开 `http://localhost.:8765/frontend/tests/iframe-regression.html`。必须保留 localhost 后的点，以使用生产 PublicAtom。实际浏览器已验证生产包的初始错误提示及输入后同轮保存消息，页面显示 PASS。

## 平台边界

此验证使用本地模拟宿主，不是厂商生产平台或 Win7 实机验证。需要发布插件新前端包才对生产生效。

`task.json` 已有必填声明；如果打开整条流水线时平台没有创建插件 iframe，插件 JavaScript 不会执行。未展开插件节点时自动标错、禁止流水线保存或运行，需要厂商在流水线加载/新增节点阶段按必填声明和插件校验规则校验。这部分宿主源码不在本仓库，不能仅靠插件 watcher 完成。

