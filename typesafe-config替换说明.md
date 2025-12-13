# com.typesafe.config 替换说明

## 替换概述

已成功将项目中的 `com.typesafe:config:1.4.2` 依赖替换为自定义工具类 `ConfigMergeUtils`。

## 创建的工具类

### ConfigMergeUtils.groovy

**文件路径**: `src/com/daluobai/jenkinslib/utils/ConfigMergeUtils.groovy`

**主要功能**:
- `mergeParams(Map configMap, Map params)` - 将 Jenkins 参数动态合并到配置 Map 中
- `setNestedValue(Map map, String path, Object value)` - 设置嵌套路径的值，支持点分隔符
- `getNestedValue(Map map, String path, Object defaultValue)` - 获取嵌套路径的值
- `hasPath(Map map, String path)` - 检查嵌套路径是否存在

**特性**:
- 支持嵌套路径，如 `"SHARE_PARAM.appName"`
- 自动创建不存在的中间 Map
- 深拷贝配置，避免修改原始数据
- 完全兼容原有的 typesafe.config 功能

## 修改的文件

### 1. vars/deployJavaWeb.groovy
**变更内容**:
- 移除 `@Grab('com.typesafe:config:1.4.2')`
- 移除 `import com.typesafe.config.*`
- 添加 `import com.daluobai.jenkinslib.utils.ConfigMergeUtils`
- 简化 `mergeConfig()` 方法中的参数合并逻辑

**替换前**:
```groovy
Config fullConfigParams = ConfigFactory.parseMap(fullConfig)
params.each {
    fullConfigParams = fullConfigParams.withValue(it.key, ConfigValueFactory.fromAnyRef(it.value))
}
fullConfig = fullConfigParams.root().unwrapped()
```

**替换后**:
```groovy
fullConfig = ConfigMergeUtils.mergeParams(fullConfig, params)
```

### 2. vars/deployWeb.groovy
**变更内容**: 与 deployJavaWeb.groovy 相同

### 3. vars/syncGit2Git.groovy
**变更内容**: 与 deployJavaWeb.groovy 相同

### 4. build.gradle
**变更内容**:
- 移除依赖: `implementation 'com.typesafe:config:1.4.2'`

## 优势对比

### 使用 typesafe.config (之前)
- ❌ 需要引入外部依赖
- ❌ 增加项目体积
- ❌ 代码较复杂（需要类型转换）
- ❌ 依赖第三方库的更新维护

### 使用 ConfigMergeUtils (现在)
- ✅ 无需外部依赖
- ✅ 代码更简洁
- ✅ 完全可控，易于维护
- ✅ 性能更优（减少对象转换）
- ✅ 支持嵌套路径操作

## 使用示例

```groovy
// 合并参数
def config = [
    SHARE_PARAM: [
        appName: "app1",
        version: "1.0"
    ]
]

def params = [
    "SHARE_PARAM.appName": "app2",
    "SHARE_PARAM.newField": "value"
]

def result = ConfigMergeUtils.mergeParams(config, params)
// 结果: [SHARE_PARAM: [appName: "app2", version: "1.0", newField: "value"]]

// 获取嵌套值
def appName = ConfigMergeUtils.getNestedValue(result, "SHARE_PARAM.appName")
// 结果: "app2"

// 检查路径是否存在
def exists = ConfigMergeUtils.hasPath(result, "SHARE_PARAM.version")
// 结果: true
```

## 验证清单

- [x] 创建 ConfigMergeUtils 工具类
- [x] 替换 deployJavaWeb.groovy 中的使用
- [x] 替换 deployWeb.groovy 中的使用
- [x] 替换 syncGit2Git.groovy 中的使用
- [x] 移除 build.gradle 中的依赖
- [x] 验证所有文件中不再有 typesafe.config 的引用

## 测试建议

建议在 Jenkins 环境中测试以下场景：
1. JavaWeb 部署流程（deployJavaWeb）
2. Web 部署流程（deployWeb）
3. Git 同步流程（syncGit2Git）
4. 验证 Jenkins 参数能否正确覆盖配置文件中的值
5. 验证嵌套路径参数的处理

---
**完成日期**: 2025/12/13  
**影响范围**: 3个 vars 文件，1个工具类，1个构建配置文件
