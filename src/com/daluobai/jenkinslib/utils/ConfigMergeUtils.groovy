package com.daluobai.jenkinslib.utils

/**
 * @author daluobai@outlook.com
 * version 1.0.0
 * @title 配置合并工具类 - 替代 com.typesafe.config
 * @description https://github.com/daluobai-devops/jenkins-shared-library
 * @create 2025/12/13
 */
class ConfigMergeUtils implements Serializable {

    /**
     * 将Jenkins参数动态合并到配置Map中
     * 支持嵌套路径，如 "SHARE_PARAM.appName"
     * @param configMap 配置Map
     * @param params Jenkins参数Map
     * @return 合并后的新Map
     */
    static Map mergeParams(Map configMap, Map params) {
        if (ObjUtils.isEmpty(params)) {
            return MapUtils.deepCopy(configMap)
        }
        
        // 深拷贝配置，避免修改原始配置
        Map result = MapUtils.deepCopy(configMap)
        
        // 遍历所有参数
        params.each { key, value ->
            // 支持嵌套路径，如 "SHARE_PARAM.appName"
            setNestedValue(result, key, value)
        }
        
        return result
    }

    /**
     * 设置嵌套路径的值
     * @param map 目标Map
     * @param path 路径，支持点分隔，如 "SHARE_PARAM.appName"
     * @param value 值
     */
    private static void setNestedValue(Map map, String path, Object value) {
        if (StrUtils.isBlank(path)) {
            return
        }
        
        // 如果路径不包含点，直接设置
        if (!path.contains('.')) {
            map.put(path, value)
            return
        }
        
        // 分割路径
        def parts = path.split('\\.')
        def current = map
        
        // 遍历路径，创建嵌套Map
        for (int i = 0; i < parts.length - 1; i++) {
            def part = parts[i]
            
            // 如果当前键不存在或不是Map，创建新Map
            if (!current.containsKey(part) || !(current[part] instanceof Map)) {
                current[part] = [:]
            }
            
            current = current[part]
        }
        
        // 设置最后一个键的值
        current.put(parts[parts.length - 1], value)
    }

    /**
     * 获取嵌套路径的值
     * @param map 源Map
     * @param path 路径，支持点分隔
     * @param defaultValue 默认值
     * @return 值
     */
    static Object getNestedValue(Map map, String path, Object defaultValue = null) {
        if (ObjUtils.isEmpty(map) || StrUtils.isBlank(path)) {
            return defaultValue
        }
        
        // 如果路径不包含点，直接获取
        if (!path.contains('.')) {
            return map.getOrDefault(path, defaultValue)
        }
        
        // 分割路径
        def parts = path.split('\\.')
        def current = map
        
        // 遍历路径
        for (def part : parts) {
            if (!(current instanceof Map) || !current.containsKey(part)) {
                return defaultValue
            }
            current = current[part]
        }
        
        return current ?: defaultValue
    }

    /**
     * 检查嵌套路径是否存在
     * @param map 源Map
     * @param path 路径
     * @return 是否存在
     */
    static boolean hasPath(Map map, String path) {
        if (ObjUtils.isEmpty(map) || StrUtils.isBlank(path)) {
            return false
        }
        
        // 如果路径不包含点，直接检查
        if (!path.contains('.')) {
            return map.containsKey(path)
        }
        
        // 分割路径
        def parts = path.split('\\.')
        def current = map
        
        // 遍历路径
        for (def part : parts) {
            if (!(current instanceof Map) || !current.containsKey(part)) {
                return false
            }
            current = current[part]
        }
        
        return true
    }
}
