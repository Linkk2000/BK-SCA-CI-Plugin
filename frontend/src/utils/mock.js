/**
 * Mock 数据服务 - 用于本地开发测试
 */

// 模拟延迟
const delay = (ms = 500) => new Promise(resolve => setTimeout(resolve, ms))

// 模拟测试连接 API
export const mockTestConnection = async (server, token) => {
    await delay(1000)
    
    // 模拟不同的响应场景
    if (!token || token.length < 10) {
        throw new Error('Token 格式不正确')
    }
    
    return {
        code: 0,
        message: "success",
        data: {
            token: "sast_mock_token_12345",
            duration: "1"  // 这个字段很重要
        }
    }
}

// 模拟项目列表 API
export const mockProjectList = async (keyword = '') => {
    await delay(800)
    
    const projects = [
        {
            projectId: "1985532746920452098",
            projectName: "ITPS",
            departmentName: "默认部门",
            createUserName: "xmirror",
            nickname: "xmirror",
            pmName: "29477",
            pmNickName: "陈含",
            appCount: 5,
            createDate: "2025-11-04 10:21:10"
        },
        {
            projectId: "1985532746920452099",
            projectName: "IT测试项目",
            departmentName: "研发部",
            createUserName: "admin",
            nickname: "admin",
            pmName: "12345",
            pmNickName: "张三",
            appCount: 3,
            createDate: "2025-11-05 14:30:00"
        },
        {
            projectId: "1985532746920452100",
            projectName: "CAP全密码平台",
            departmentName: "安全部",
            createUserName: "security",
            nickname: "security",
            pmName: "67890",
            pmNickName: "李四",
            appCount: 8,
            createDate: "2025-11-03 09:15:00"
        },
        {
            projectId: "1985532746920452101",
            projectName: "DMS数据管理系统",
            departmentName: "数据部",
            createUserName: "dataadmin",
            nickname: "dataadmin",
            pmName: "11111",
            pmNickName: "王五",
            appCount: 12,
            createDate: "2025-11-02 16:45:00"
        },
        {
            projectId: "1985532746920452102",
            projectName: "统一认证平台",
            departmentName: "架构部",
            createUserName: "architect",
            nickname: "architect",
            pmName: "22222",
            pmNickName: "赵六",
            appCount: 6,
            createDate: "2025-11-01 11:20:00"
        }
    ]
    
    // 根据关键字过滤
    let filtered = projects
    if (keyword) {
        filtered = projects.filter(p => 
            p.projectName.toLowerCase().includes(keyword.toLowerCase())
        )
    }
    
    return {
        code: 0,
        message: "success",
        data: {
            records: filtered,
            total: filtered.length,
            size: 20,
            current: 1,
            pages: 1
        }
    }
}

// 模拟应用列表 API
export const mockAppList = async (projectId, keyword = '') => {
    await delay(800)
    
    // 不同项目的应用列表
    const appsMap = {
        "1985532746920452098": [
            {
                appId: "1985634487142813698",
                taskId: "1985634544457977858",
                status: 3,
                appName: "全密码服务门户-压缩包格式",
                superCount: 106,
                highCount: 293,
                middleCount: 650,
                lowCount: 1687,
                totalCount: 2736,
                repositoryType: 0,
                codeSource: "cap-init_source",
                languageList: [{ id: 1, name: "Java/Android/JSP" }],
                createDate: "2025-11-04 17:05:27",
                createUser: "xmirror",
                nickname: "xmirror"
            },
            {
                appId: "1985632338023387138",
                taskId: "1985632338258268162",
                status: 4,
                appName: "Gitlab-Pull-Test",
                superCount: 0,
                highCount: 0,
                middleCount: 0,
                lowCount: 0,
                totalCount: 0,
                repositoryType: 2,
                codeSource: "http://172.25.160.168/zhicheng/sharedservice/csp/csp/cap",
                languageList: [{ id: 1, name: "Java/Android/JSP" }],
                createDate: "2025-11-04 16:56:55",
                createUser: "admin",
                nickname: "admin"
            },
            {
                appId: "1985619901345796097",
                taskId: "1985619901584871425",
                status: 3,
                appName: "DMS-XC-对照",
                superCount: 0,
                highCount: 0,
                middleCount: 0,
                lowCount: 0,
                totalCount: 0,
                repositoryType: 2,
                codeSource: "http://172.25.160.168/qudao/DMS-XC/code",
                languageList: [{ id: 1, name: "Java/Android/JSP" }],
                createDate: "2025-11-04 16:07:30",
                createUser: "29477",
                nickname: "陈含"
            }
        ],
        "1985532746920452099": [
            {
                appId: "2000000000000000001",
                appName: "测试应用A",
                status: 3,
                totalCount: 150,
                createDate: "2025-11-05 14:30:00",
                createUser: "admin"
            },
            {
                appId: "2000000000000000002",
                appName: "测试应用B",
                status: 3,
                totalCount: 89,
                createDate: "2025-11-05 15:00:00",
                createUser: "admin"
            }
        ],
        "1985532746920452100": [
            {
                appId: "3000000000000000001",
                appName: "CAP-认证服务",
                status: 3,
                totalCount: 456,
                createDate: "2025-11-03 09:15:00",
                createUser: "security"
            },
            {
                appId: "3000000000000000002",
                appName: "CAP-授权服务",
                status: 3,
                totalCount: 321,
                createDate: "2025-11-03 10:30:00",
                createUser: "security"
            },
            {
                appId: "3000000000000000003",
                appName: "CAP-密钥管理",
                status: 3,
                totalCount: 234,
                createDate: "2025-11-03 11:00:00",
                createUser: "security"
            }
        ]
    }
    
    let apps = appsMap[projectId] || []
    
    // 如果没有找到项目的应用，返回默认应用
    if (apps.length === 0) {
        apps = [
            {
                appId: "9999999999999999999",
                appName: `默认应用-项目${projectId}`,
                status: 3,
                totalCount: 0,
                createDate: "2025-11-01 00:00:00",
                createUser: "system"
            }
        ]
    }
    
    // 根据关键字过滤
    if (keyword) {
        apps = apps.filter(a => 
            a.appName.toLowerCase().includes(keyword.toLowerCase())
        )
    }
    
    return {
        code: 0,
        message: "success",
        data: {
            records: apps,
            total: apps.length,
            size: 20,
            current: 1,
            pages: 1
        }
    }
}

// 统一的 Mock Ajax 方法
export const mockAjax = async (config) => {
    const { url, method = 'GET' } = config
    
    console.log('[Mock API] Call:', { url, method })
    
    // 解析 URL
    try {
        const urlObj = new URL(url, 'http://mock.local')
        const pathname = urlObj.pathname
        const searchParams = urlObj.searchParams
        
        // 测试连接接口
        if (pathname.includes('/token/connect')) {
            const token = searchParams.get('token')
            return await mockTestConnection(url, token)
        }
        
        // 项目列表接口
        if (pathname.includes('/project/page')) {
            const keyword = searchParams.get('contParam') || ''
            return await mockProjectList(keyword)
        }
        
        // 应用列表接口
        if (pathname.includes('/app/info/')) {
            const match = pathname.match(/\/app\/info\/(\d+)/)
            const projectId = match ? match[1] : ''
            return await mockAppList(projectId)
        }
        
        // 未匹配的接口
        throw new Error(`未找到匹配的 Mock 接口: ${pathname}`)
        
    } catch (error) {
        console.error('Mock API Error:', error)
        throw error
    }
}

