package com.daluobai.jenkinslib.utils

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovy.json.JsonSlurperClassic

/**
 * @author daluobai@outlook.com
 * version 1.0.0
 * @title JSON工具类 - 参考hutool JSONUtil
 * @description https://github.com/daluobai-devops/jenkins-shared-library
 * @create 2025/12/13
 */
class JsonUtils implements Serializable {

    /**
     * 判断字符串是否为JSON格式
     * @param str 字符串
     * @return 是否为JSON
     */
    static boolean isJson(String str) {
        if (StrUtils.isBlank(str)) {
            return false
        }
        
        str = str.trim()
        return (str.startsWith("{") && str.endsWith("}")) || (str.startsWith("[") && str.endsWith("]"))
    }

    /**
     * 判断字符串是否为JSON对象
     * @param str 字符串
     * @return 是否为JSON对象
     */
    static boolean isJsonObj(String str) {
        if (StrUtils.isBlank(str)) {
            return false
        }
        
        str = str.trim()
        return str.startsWith("{") && str.endsWith("}")
    }

    /**
     * 判断字符串是否为JSON数组
     * @param str 字符串
     * @return 是否为JSON数组
     */
    static boolean isJsonArray(String str) {
        if (StrUtils.isBlank(str)) {
            return false
        }
        
        str = str.trim()
        return str.startsWith("[") && str.endsWith("]")
    }

    /**
     * JSON字符串转为对象
     * @param jsonStr JSON字符串
     * @return 对象（可能是Map或List）
     */
    static Object parse(String jsonStr) {
        if (StrUtils.isBlank(jsonStr)) {
            return null
        }
        
        try {
            return new JsonSlurperClassic().parseText(jsonStr)
        } catch (Exception e) {
            throw new RuntimeException("JSON解析失败", e)
        }
    }

    /**
     * JSON字符串转为Map
     * @param jsonStr JSON字符串
     * @return Map对象
     */
    static Map<String, Object> parseObj(String jsonStr) {
        Object result = parse(jsonStr)
        if (result instanceof Map) {
            return (Map<String, Object>) result
        }
        throw new IllegalArgumentException("JSON字符串不是一个对象")
    }

    /**
     * JSON字符串转为List
     * @param jsonStr JSON字符串
     * @return List对象
     */
    static List<Object> parseArray(String jsonStr) {
        Object result = parse(jsonStr)
        if (result instanceof List) {
            return (List<Object>) result
        }
        throw new IllegalArgumentException("JSON字符串不是一个数组")
    }

    /**
     * 对象转JSON字符串
     * @param obj 对象
     * @return JSON字符串
     */
    static String toJsonStr(Object obj) {
        if (obj == null) {
            return "null"
        }
        
        try {
            return JsonOutput.toJson(obj)
        } catch (Exception e) {
            throw new RuntimeException("对象转JSON失败", e)
        }
    }

    /**
     * 对象转JSON字符串（格式化）
     * @param obj 对象
     * @return 格式化的JSON字符串
     */
    static String toJsonPrettyStr(Object obj) {
        if (obj == null) {
            return "null"
        }
        
        try {
            return JsonOutput.prettyPrint(JsonOutput.toJson(obj))
        } catch (Exception e) {
            throw new RuntimeException("对象转JSON失败", e)
        }
    }

    /**
     * JSONObject类 - 包装Map提供便捷方法
     */
    static class JSONObject {
        private Map<String, Object> map

        JSONObject() {
            this.map = [:]
        }

        JSONObject(Map<String, Object> map) {
            this.map = map ?: [:]
        }

        JSONObject(String jsonStr) {
            this.map = parseObj(jsonStr)
        }

        /**
         * 获取字符串值
         */
        String getStr(String key) {
            Object value = map.get(key)
            return value == null ? null : value.toString()
        }

        /**
         * 获取整数值
         */
        Integer getInt(String key) {
            Object value = map.get(key)
            if (value == null) {
                return null
            }
            if (value instanceof Number) {
                return ((Number) value).intValue()
            }
            return Integer.parseInt(value.toString())
        }

        /**
         * 获取长整数值
         */
        Long getLong(String key) {
            Object value = map.get(key)
            if (value == null) {
                return null
            }
            if (value instanceof Number) {
                return ((Number) value).longValue()
            }
            return Long.parseLong(value.toString())
        }

        /**
         * 获取布尔值
         */
        Boolean getBool(String key) {
            Object value = map.get(key)
            if (value == null) {
                return null
            }
            if (value instanceof Boolean) {
                return (Boolean) value
            }
            return Boolean.parseBoolean(value.toString())
        }

        /**
         * 获取双精度浮点数值
         */
        Double getDouble(String key) {
            Object value = map.get(key)
            if (value == null) {
                return null
            }
            if (value instanceof Number) {
                return ((Number) value).doubleValue()
            }
            return Double.parseDouble(value.toString())
        }

        /**
         * 获取JSONObject
         */
        JSONObject getJSONObject(String key) {
            Object value = map.get(key)
            if (value == null) {
                return null
            }
            if (value instanceof Map) {
                return new JSONObject((Map<String, Object>) value)
            }
            throw new IllegalArgumentException("值不是JSONObject类型")
        }

        /**
         * 获取JSONArray
         */
        JSONArray getJSONArray(String key) {
            Object value = map.get(key)
            if (value == null) {
                return null
            }
            if (value instanceof List) {
                return new JSONArray((List<Object>) value)
            }
            throw new IllegalArgumentException("值不是JSONArray类型")
        }

        /**
         * 设置值
         */
        JSONObject set(String key, Object value) {
            map.put(key, value)
            return this
        }

        /**
         * 获取值
         */
        Object get(String key) {
            return map.get(key)
        }

        /**
         * 判断是否包含key
         */
        boolean containsKey(String key) {
            return map.containsKey(key)
        }

        /**
         * 转为JSON字符串
         */
        String toString() {
            return toJsonStr(map)
        }

        /**
         * 获取底层Map
         */
        Map<String, Object> toMap() {
            return new HashMap<>(map)
        }
    }

    /**
     * JSONArray类 - 包装List提供便捷方法
     */
    static class JSONArray {
        private List<Object> list

        JSONArray() {
            this.list = []
        }

        JSONArray(List<Object> list) {
            this.list = list ?: []
        }

        JSONArray(String jsonStr) {
            this.list = parseArray(jsonStr)
        }

        /**
         * 获取指定索引的元素
         */
        Object get(int index) {
            return list.get(index)
        }

        /**
         * 获取字符串值
         */
        String getStr(int index) {
            Object value = list.get(index)
            return value == null ? null : value.toString()
        }

        /**
         * 获取整数值
         */
        Integer getInt(int index) {
            Object value = list.get(index)
            if (value == null) {
                return null
            }
            if (value instanceof Number) {
                return ((Number) value).intValue()
            }
            return Integer.parseInt(value.toString())
        }

        /**
         * 获取JSONObject
         */
        JSONObject getJSONObject(int index) {
            Object value = list.get(index)
            if (value == null) {
                return null
            }
            if (value instanceof Map) {
                return new JSONObject((Map<String, Object>) value)
            }
            throw new IllegalArgumentException("值不是JSONObject类型")
        }

        /**
         * 添加元素
         */
        JSONArray add(Object value) {
            list.add(value)
            return this
        }

        /**
         * 获取大小
         */
        int size() {
            return list.size()
        }

        /**
         * 转为JSON字符串
         */
        String toString() {
            return toJsonStr(list)
        }

        /**
         * 获取底层List
         */
        List<Object> toList() {
            return new ArrayList<>(list)
        }
    }
}
