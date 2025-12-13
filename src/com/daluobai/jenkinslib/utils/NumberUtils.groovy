package com.daluobai.jenkinslib.utils

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * @author daluobai@outlook.com
 * version 1.0.0
 * @title 数字工具类 - 参考hutool NumberUtil
 * @description https://github.com/daluobai-devops/jenkins-shared-library
 * @create 2025/12/13
 */
class NumberUtils implements Serializable {

    /**
     * 判断字符串是否为数字
     * @param str 字符串
     * @return 是否为数字
     */
    static boolean isNumber(String str) {
        if (StrUtils.isBlank(str)) {
            return false
        }
        
        try {
            new BigDecimal(str)
            return true
        } catch (NumberFormatException e) {
            return false
        }
    }

    /**
     * 判断字符串是否为整数
     * @param str 字符串
     * @return 是否为整数
     */
    static boolean isInteger(String str) {
        if (StrUtils.isBlank(str)) {
            return false
        }
        
        try {
            Integer.parseInt(str)
            return true
        } catch (NumberFormatException e) {
            return false
        }
    }

    /**
     * 判断字符串是否为Long
     * @param str 字符串
     * @return 是否为Long
     */
    static boolean isLong(String str) {
        if (StrUtils.isBlank(str)) {
            return false
        }
        
        try {
            Long.parseLong(str)
            return true
        } catch (NumberFormatException e) {
            return false
        }
    }

    /**
     * 判断字符串是否为Double
     * @param str 字符串
     * @return 是否为Double
     */
    static boolean isDouble(String str) {
        if (StrUtils.isBlank(str)) {
            return false
        }
        
        try {
            Double.parseDouble(str)
            return true
        } catch (NumberFormatException e) {
            return false
        }
    }

    /**
     * 字符串转Integer
     * @param str 字符串
     * @param defaultValue 默认值
     * @return Integer
     */
    static Integer parseInt(String str, Integer defaultValue = null) {
        if (StrUtils.isBlank(str)) {
            return defaultValue
        }
        
        try {
            return Integer.parseInt(str.trim())
        } catch (NumberFormatException e) {
            return defaultValue
        }
    }

    /**
     * 字符串转Long
     * @param str 字符串
     * @param defaultValue 默认值
     * @return Long
     */
    static Long parseLong(String str, Long defaultValue = null) {
        if (StrUtils.isBlank(str)) {
            return defaultValue
        }
        
        try {
            return Long.parseLong(str.trim())
        } catch (NumberFormatException e) {
            return defaultValue
        }
    }

    /**
     * 字符串转Double
     * @param str 字符串
     * @param defaultValue 默认值
     * @return Double
     */
    static Double parseDouble(String str, Double defaultValue = null) {
        if (StrUtils.isBlank(str)) {
            return defaultValue
        }
        
        try {
            return Double.parseDouble(str.trim())
        } catch (NumberFormatException e) {
            return defaultValue
        }
    }

    /**
     * 字符串转Float
     * @param str 字符串
     * @param defaultValue 默认值
     * @return Float
     */
    static Float parseFloat(String str, Float defaultValue = null) {
        if (StrUtils.isBlank(str)) {
            return defaultValue
        }
        
        try {
            return Float.parseFloat(str.trim())
        } catch (NumberFormatException e) {
            return defaultValue
        }
    }

    /**
     * 字符串转BigDecimal
     * @param str 字符串
     * @param defaultValue 默认值
     * @return BigDecimal
     */
    static BigDecimal parseBigDecimal(String str, BigDecimal defaultValue = null) {
        if (StrUtils.isBlank(str)) {
            return defaultValue
        }
        
        try {
            return new BigDecimal(str.trim())
        } catch (NumberFormatException e) {
            return defaultValue
        }
    }

    /**
     * 加法运算
     * @param v1 被加数
     * @param v2 加数
     * @return 和
     */
    static BigDecimal add(Number v1, Number v2) {
        if (v1 == null && v2 == null) {
            return BigDecimal.ZERO
        }
        
        BigDecimal b1 = v1 == null ? BigDecimal.ZERO : new BigDecimal(v1.toString())
        BigDecimal b2 = v2 == null ? BigDecimal.ZERO : new BigDecimal(v2.toString())
        return b1.add(b2)
    }

    /**
     * 减法运算
     * @param v1 被减数
     * @param v2 减数
     * @return 差
     */
    static BigDecimal sub(Number v1, Number v2) {
        if (v1 == null && v2 == null) {
            return BigDecimal.ZERO
        }
        
        BigDecimal b1 = v1 == null ? BigDecimal.ZERO : new BigDecimal(v1.toString())
        BigDecimal b2 = v2 == null ? BigDecimal.ZERO : new BigDecimal(v2.toString())
        return b1.subtract(b2)
    }

    /**
     * 乘法运算
     * @param v1 被乘数
     * @param v2 乘数
     * @return 积
     */
    static BigDecimal mul(Number v1, Number v2) {
        if (v1 == null || v2 == null) {
            return BigDecimal.ZERO
        }
        
        BigDecimal b1 = new BigDecimal(v1.toString())
        BigDecimal b2 = new BigDecimal(v2.toString())
        return b1.multiply(b2)
    }

    /**
     * 除法运算
     * @param v1 被除数
     * @param v2 除数
     * @param scale 保留小数位数
     * @return 商
     */
    static BigDecimal div(Number v1, Number v2, int scale = 2) {
        if (v1 == null || v2 == null) {
            return BigDecimal.ZERO
        }
        
        if (v2.doubleValue() == 0) {
            throw new ArithmeticException("除数不能为0")
        }
        
        BigDecimal b1 = new BigDecimal(v1.toString())
        BigDecimal b2 = new BigDecimal(v2.toString())
        return b1.divide(b2, scale, RoundingMode.HALF_UP)
    }

    /**
     * 四舍五入
     * @param value 值
     * @param scale 保留小数位数
     * @return 结果
     */
    static BigDecimal round(Number value, int scale = 2) {
        if (value == null) {
            return BigDecimal.ZERO
        }
        
        BigDecimal b = new BigDecimal(value.toString())
        return b.setScale(scale, RoundingMode.HALF_UP)
    }

    /**
     * 向上取整
     * @param value 值
     * @param scale 保留小数位数
     * @return 结果
     */
    static BigDecimal roundUp(Number value, int scale = 2) {
        if (value == null) {
            return BigDecimal.ZERO
        }
        
        BigDecimal b = new BigDecimal(value.toString())
        return b.setScale(scale, RoundingMode.UP)
    }

    /**
     * 向下取整
     * @param value 值
     * @param scale 保留小数位数
     * @return 结果
     */
    static BigDecimal roundDown(Number value, int scale = 2) {
        if (value == null) {
            return BigDecimal.ZERO
        }
        
        BigDecimal b = new BigDecimal(value.toString())
        return b.setScale(scale, RoundingMode.DOWN)
    }

    /**
     * 比较大小
     * @param v1 数值1
     * @param v2 数值2
     * @return v1 > v2 返回1, v1 == v2 返回0, v1 < v2 返回-1
     */
    static int compare(Number v1, Number v2) {
        BigDecimal b1 = new BigDecimal(v1.toString())
        BigDecimal b2 = new BigDecimal(v2.toString())
        return b1.compareTo(b2)
    }

    /**
     * 判断是否相等
     * @param v1 数值1
     * @param v2 数值2
     * @return 是否相等
     */
    static boolean equals(Number v1, Number v2) {
        return compare(v1, v2) == 0
    }

    /**
     * 判断v1是否大于v2
     * @param v1 数值1
     * @param v2 数值2
     * @return v1 > v2
     */
    static boolean isGreater(Number v1, Number v2) {
        return compare(v1, v2) > 0
    }

    /**
     * 判断v1是否大于等于v2
     * @param v1 数值1
     * @param v2 数值2
     * @return v1 >= v2
     */
    static boolean isGreaterOrEqual(Number v1, Number v2) {
        return compare(v1, v2) >= 0
    }

    /**
     * 判断v1是否小于v2
     * @param v1 数值1
     * @param v2 数值2
     * @return v1 < v2
     */
    static boolean isLess(Number v1, Number v2) {
        return compare(v1, v2) < 0
    }

    /**
     * 判断v1是否小于等于v2
     * @param v1 数值1
     * @param v2 数值2
     * @return v1 <= v2
     */
    static boolean isLessOrEqual(Number v1, Number v2) {
        return compare(v1, v2) <= 0
    }

    /**
     * 获取最大值
     * @param numbers 数字数组
     * @return 最大值
     */
    static Number max(Number... numbers) {
        if (numbers == null || numbers.length == 0) {
            return null
        }
        
        Number max = numbers[0]
        for (int i = 1; i < numbers.length; i++) {
            if (numbers[i] != null && compare(numbers[i], max) > 0) {
                max = numbers[i]
            }
        }
        return max
    }

    /**
     * 获取最小值
     * @param numbers 数字数组
     * @return 最小值
     */
    static Number min(Number... numbers) {
        if (numbers == null || numbers.length == 0) {
            return null
        }
        
        Number min = numbers[0]
        for (int i = 1; i < numbers.length; i++) {
            if (numbers[i] != null && compare(numbers[i], min) < 0) {
                min = numbers[i]
            }
        }
        return min
    }

    /**
     * 求和
     * @param numbers 数字数组
     * @return 总和
     */
    static BigDecimal sum(Number... numbers) {
        if (numbers == null || numbers.length == 0) {
            return BigDecimal.ZERO
        }
        
        BigDecimal sum = BigDecimal.ZERO
        for (Number num : numbers) {
            if (num != null) {
                sum = sum.add(new BigDecimal(num.toString()))
            }
        }
        return sum
    }

    /**
     * 求平均值
     * @param numbers 数字数组
     * @param scale 保留小数位数
     * @return 平均值
     */
    static BigDecimal average(Number[] numbers, int scale = 2) {
        if (numbers == null || numbers.length == 0) {
            return BigDecimal.ZERO
        }
        
        BigDecimal sum = sum(numbers)
        return div(sum, numbers.length, scale)
    }

    /**
     * 判断数值是否在指定范围内
     * @param value 值
     * @param min 最小值（包含）
     * @param max 最大值（包含）
     * @return 是否在范围内
     */
    static boolean isInRange(Number value, Number min, Number max) {
        if (value == null) {
            return false
        }
        
        return compare(value, min) >= 0 && compare(value, max) <= 0
    }

    /**
     * 数字格式化
     * @param value 值
     * @param format 格式（如：#.00）
     * @return 格式化后的字符串
     */
    static String format(Number value, String format = "#.00") {
        if (value == null) {
            return "0"
        }
        
        try {
            java.text.DecimalFormat df = new java.text.DecimalFormat(format)
            return df.format(value)
        } catch (Exception e) {
            return value.toString()
        }
    }

    /**
     * 百分比格式化
     * @param value 值
     * @param scale 保留小数位数
     * @return 百分比字符串（如：12.34%）
     */
    static String formatPercent(Number value, int scale = 2) {
        if (value == null) {
            return "0%"
        }
        
        BigDecimal percent = mul(value, 100)
        return format(percent, "#." + "0" * scale) + "%"
    }
}
