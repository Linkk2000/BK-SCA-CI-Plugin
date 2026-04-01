# 自动化构建说明

## 🚀 一键打包

现在你只需要在 `pipeline-plugin/SastTest` 目录下执行：

```bash
mvn clean package
```

## 📦 自动化构建流程

Maven 会自动完成以下步骤：

1. **安装 Node.js 和 npm**（如果本地没有）
2. **执行 `npm install`**（安装前端依赖）
3. **执行 `npm run public`**（打包前端代码）
4. **复制前端打包文件**到 `target/frontend/`
5. **编译 Java 代码**
6. **打包 jar-with-dependencies**
7. **生成最终 zip 包**

## 📁 构建产物

构建完成后，在 `target` 目录下会生成：

```
target/
├── SastTest.jar                           # 普通 jar
├── SastTest-jar-with-dependencies.jar     # 包含所有依赖的 jar
├── SastTest-release.zip                   # 最终发布包 ⭐
└── frontend/                              # 前端打包文件
    ├── index.html
    ├── remoteAtom.xxx.js
    └── remoteAtom.xxx.js.map
```

## ⭐ 发布包

**`SastTest-release.zip`** 就是最终的发布包，解压后结构：

```
SastTest-release.zip
└── SastTest/                              ← 根目录
    ├── frontend/
    │   ├── index.html
    │   └── remoteAtom.xxx.js
    ├── task.json
    └── SastTest-jar-with-dependencies.jar
```

直接上传这个 zip 包到 BKCI 研发商店即可！

## 🔧 配置说明

### 前端代码路径

在 `pom.xml` 中配置：
```xml
<frontend.directory>../../frontend</frontend.directory>
```

如果前端代码在其他位置，修改这个路径即可。

### Node 版本

默认使用 Node v16.20.0，如需修改：
```xml
<nodeVersion>v16.20.0</nodeVersion>
<npmVersion>8.19.4</npmVersion>
```

## 🎯 快速开始

```bash
# 进入插件目录
cd pipeline-plugin/SastTest

# 打包（首次会下载 Node，需要等待）
mvn clean package

# 上传 target/SastTest-release.zip 到研发商店
```

## 📝 注意事项

1. 首次构建会下载 Node.js，可能需要几分钟
2. 确保前端代码路径正确
3. 确保 task.json 在插件根目录
4. 如果修改了前端代码，只需重新 `mvn package` 即可

