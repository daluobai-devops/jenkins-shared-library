package com.daluobai.jenkinslib.utils

/**
 * @author daluobai@outlook.com
 * version 1.0.0
 * @title 对象工具类 - 参考hutool ObjectUtil
 * @description https://github.com/daluobai-devops/jenkins-shared-library
 * @create 2025/12/13
 */
class ObjUtils implements Serializable {

    /**
     * 判断对象是否为null
     * @param obj 对象
     * @return 是否为null
     */
    static boolean isNull(Object obj) {
        return obj == null
    }

    /**
     * 判断对象是否不为null
     * @param obj 对象
     * @return 是否不为null
     */
    static boolean isNotNull(Object obj) {
        return obj != null
    }

    /**
     * 判断对象是否为空
     * 支持：String、Collection、Map、Array
     * @param obj 对象
     * @return 是否为空
     */
    static boolean isEmpty(Object obj) {
        if (obj == null) {
            return true
        }
        
        if (obj instanceof CharSequence) {
            return ((CharSequence) obj).length() == 0
        }
        
        if (obj instanceof Collection) {
            return ((Collection) obj).isEmpty()
        }
        
        if (obj instanceof Map) {
            return ((Map) obj).isEmpty()
        }
        
        if (obj.getClass().isArray()) {
            return ((Object[]) obj).length == 0
        }
        
        return false
    }

    /**
     * 判断对象是否不为空
     * @param obj 对象
     * @return 是否不为空
     */
    static boolean isNotEmpty(Object obj) {
        return !isEmpty(obj)
    }

    /**
     * 如果对象为null，返回默认值
     * @param obj 对象
     * @param defaultValue 默认值
     * @return 对象本身或默认值
     */
    static <T> T defaultIfNull(T obj, T defaultValue) {
        return obj == null ? defaultValue : obj
    }

    /**
     * 如果对象为空，返回默认值
     * @param obj 对象
     * @param defaultValue 默认值
     * @return 对象本身或默认值
     */
    static <T> T defaultIfEmpty(T obj, T defaultValue) {
        return isEmpty(obj) ? defaultValue : obj
    }

    /**
     * 判断对象是否相等
     * @param obj1 对象1
     * @param obj2 对象2
     * @return 是否相等
     */
    static boolean equals(Object obj1, Object obj2) {
        if (obj1 == obj2) {
            return true
        }
        if (obj1 == null || obj2 == null) {
            return false
        }
        return obj1.equals(obj2)
    }

    /**
     * 判断对象是否不相等
     * @param obj1 对象1
     * @param obj2 对象2
     * @return 是否不相等
     */
    static boolean notEquals(Object obj1, Object obj2) {
        return !equals(obj1, obj2)
    }

    /**
     * 克隆对象
     * @param obj 对象
     * @return 克隆后的对象
     */
    static <T> T clone(T obj) {
        if (obj == null) {
            return null
        }
        
        if (obj instanceof Cloneable) {
            return (T) obj.clone()
        }
        
        throw new IllegalArgumentException("对象不支持克隆")
    }

    /**
     * 获取对象的字符串表示
     * @param obj 对象
     * @return 字符串表示
     */
    static String toString(Object obj) {
        return obj == null ? null : obj.toString()
    }

    /**
     * 获取对象的字符串表示，如果为null则返回默认值
     * @param obj 对象
     * @param defaultStr 默认值
     * @return 字符串表示
     */
    static String toString(Object obj, String defaultStr) {
        return obj == null ? defaultStr : obj.toString()
    }

    /**
     * 判断对象是否为基本类型
     * @param obj 对象
     * @return 是否为基本类型
     */
    static boolean isBasicType(Object obj) {
        if (obj == null) {
            return false
        }
        
        Class<?> clazz = obj.getClass()
        return clazz.isPrimitive() || 
               obj instanceof String ||
               obj instanceof Number ||
               obj instanceof Boolean ||
               obj instanceof Character
    }

    /**
     * 将对象转为指定类型
     * @param obj 对象
     * @param targetClass 目标类型
     * @return 转换后的对象
     */
    static <T> T convert(Object obj, Class<T> targetClass) {
        if (obj == null) {
            return null
        }
        
        if (targetClass.isInstance(obj)) {
            return (T) obj
        }
        
        // String转换
        if (targetClass == String.class) {
            return (T) obj.toString()
        }
        
        // 数字转换
        if (obj instanceof Number) {
            Number number = (Number) obj
            if (targetClass == Integer.class || targetClass == int.class) {
                return (T) Integer.valueOf(number.intValue())
            }
            if (targetClass == Long.class || targetClass == long.class) {
                return (T) Long.valueOf(number.longValue())
            }
            if (targetClass == Double.class || targetClass == double.class) {
                return (T) Double.valueOf(number.doubleValue())
            }
            if (targetClass == Float.class || targetClass == float.class) {
                return (T) Float.valueOf(number.floatValue())
            }
            if (targetClass == Short.class || targetClass == short.class) {
                return (T) Short.valueOf(number.shortValue())
            }
            if (targetClass == Byte.class || targetClass == byte.class) {
                return (T) Byte.valueOf(number.byteValue())
            }
        }
        
        // 字符串转数字
        if (obj instanceof String) {
            String str = (String) obj
            if (targetClass == Integer.class || targetClass == int.class) {
                return (T) Integer.valueOf(str)
            }
            if (targetClass == Long.class || targetClass == long.class) {
                return (T) Long.valueOf(str)
            }
            if (targetClass == Double.class || targetClass == double.class) {
                return (T) Double.valueOf(str)
            }
            if (targetClass == Float.class || targetClass == float.class) {
                return (T) Float.valueOf(str)
            }
            if (targetClass == Boolean.class || targetClass == boolean.class) {
                return (T) Boolean.valueOf(str)
            }
        }
        
        throw new IllegalArgumentException("不支持的类型转换: " + obj.getClass() + " -> " + targetClass)
    }

    /**
     * 判断集合中是否包含指定元素
     * @param collection 集合
     * @param element 元素
     * @return 是否包含
     */
    static boolean contains(Collection<?> collection, Object element) {
        if (collection == null || collection.isEmpty()) {
            return false
        }
        return collection.contains(element)
    }

    /**
     * 判断数组中是否包含指定元素
     * @param array 数组
     * @param element 元素
     * @return 是否包含
     */
    static boolean contains(Object[] array, Object element) {
        if (array == null || array.length == 0) {
            return false
        }
        
        for (Object item : array) {
            if (equals(item, element)) {
                return true
            }
        }
        
        return false
    }
}
