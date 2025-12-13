package com.daluobai.jenkinslib.utils

/**
 * @author daluobai@outlook.com
 * version 1.0.0
 * @title 断言工具类 - 参考hutool Assert
 * @description https://github.com/daluobai-devops/jenkins-shared-library
 * @create 2025/12/13
 */
class AssertUtils implements Serializable {

    /**
     * 断言对象不为null
     * @param obj 被检查的对象
     * @param errorMsgTemplate 异常信息模板
     * @param params 异常信息参数
     * @throws IllegalArgumentException 如果对象为null
     */
    static void notNull(Object obj, String errorMsgTemplate = "对象为null", Object... params) {
        if (obj == null) {
            throw new IllegalArgumentException(StrUtils.format(errorMsgTemplate, params))
        }
    }

    /**
     * 断言字符串不为空白
     * @param text 被检查的字符串
     * @param errorMsgTemplate 异常信息模板
     * @param params 异常信息参数
     * @throws IllegalArgumentException 如果字符串为空白
     */
    static void notBlank(CharSequence text, String errorMsgTemplate = "字符串为空白", Object... params) {
        if (StrUtils.isBlank(text)) {
            throw new IllegalArgumentException(StrUtils.format(errorMsgTemplate, params))
        }
    }

    /**
     * 断言字符串不为空
     * @param text 被检查的字符串
     * @param errorMsgTemplate 异常信息模板
     * @param params 异常信息参数
     * @throws IllegalArgumentException 如果字符串为空
     */
    static void notEmpty(CharSequence text, String errorMsgTemplate = "字符串为空", Object... params) {
        if (StrUtils.isEmpty(text)) {
            throw new IllegalArgumentException(StrUtils.format(errorMsgTemplate, params))
        }
    }

    /**
     * 断言集合不为空
     * @param collection 被检查的集合
     * @param errorMsgTemplate 异常信息模板
     * @param params 异常信息参数
     * @throws IllegalArgumentException 如果集合为空
     */
    static void notEmpty(Collection<?> collection, String errorMsgTemplate = "集合为空", Object... params) {
        if (collection == null || collection.isEmpty()) {
            throw new IllegalArgumentException(StrUtils.format(errorMsgTemplate, params))
        }
    }

    /**
     * 断言Map不为空
     * @param map 被检查的Map
     * @param errorMsgTemplate 异常信息模板
     * @param params 异常信息参数
     * @throws IllegalArgumentException 如果Map为空
     */
    static void notEmpty(Map<?, ?> map, String errorMsgTemplate = "Map为空", Object... params) {
        if (map == null || map.isEmpty()) {
            throw new IllegalArgumentException(StrUtils.format(errorMsgTemplate, params))
        }
    }

    /**
     * 断言数组不为空
     * @param array 被检查的数组
     * @param errorMsgTemplate 异常信息模板
     * @param params 异常信息参数
     * @throws IllegalArgumentException 如果数组为空
     */
    static void notEmpty(Object[] array, String errorMsgTemplate = "数组为空", Object... params) {
        if (array == null || array.length == 0) {
            throw new IllegalArgumentException(StrUtils.format(errorMsgTemplate, params))
        }
    }

    /**
     * 断言表达式为true
     * @param expression 表达式
     * @param errorMsgTemplate 异常信息模板
     * @param params 异常信息参数
     * @throws IllegalArgumentException 如果表达式为false
     */
    static void isTrue(boolean expression, String errorMsgTemplate = "表达式为false", Object... params) {
        if (!expression) {
            throw new IllegalArgumentException(StrUtils.format(errorMsgTemplate, params))
        }
    }

    /**
     * 断言表达式为false
     * @param expression 表达式
     * @param errorMsgTemplate 异常信息模板
     * @param params 异常信息参数
     * @throws IllegalArgumentException 如果表达式为true
     */
    static void isFalse(boolean expression, String errorMsgTemplate = "表达式为true", Object... params) {
        if (expression) {
            throw new IllegalArgumentException(StrUtils.format(errorMsgTemplate, params))
        }
    }

    /**
     * 断言两个对象相等
     * @param obj1 对象1
     * @param obj2 对象2
     * @param errorMsgTemplate 异常信息模板
     * @param params 异常信息参数
     * @throws IllegalArgumentException 如果两个对象不相等
     */
    static void equals(Object obj1, Object obj2, String errorMsgTemplate = "对象不相等", Object... params) {
        if (obj1 == null) {
            if (obj2 != null) {
                throw new IllegalArgumentException(StrUtils.format(errorMsgTemplate, params))
            }
        } else if (!obj1.equals(obj2)) {
            throw new IllegalArgumentException(StrUtils.format(errorMsgTemplate, params))
        }
    }

    /**
     * 断言对象在指定范围内
     * @param value 值
     * @param min 最小值（包含）
     * @param max 最大值（包含）
     * @param errorMsgTemplate 异常信息模板
     * @param params 异常信息参数
     * @throws IllegalArgumentException 如果值不在范围内
     */
    static void between(long value, long min, long max, String errorMsgTemplate = "值不在范围内", Object... params) {
        if (value < min || value > max) {
            throw new IllegalArgumentException(StrUtils.format(errorMsgTemplate, params))
        }
    }
}
