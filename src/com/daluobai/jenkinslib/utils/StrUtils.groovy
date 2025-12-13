package com.daluobai.jenkinslib.utils

/**
 * @author daluobai@outlook.com
 * version 1.0.0
 * @title 字符串工具类 - 参考hutool StrUtil
 * @description https://github.com/daluobai-devops/jenkins-shared-library
 * @create 2025/12/13
 */
class StrUtils implements Serializable {

    /**
     * 字符串是否为空白
     * @param str 字符串
     * @return 是否为空白
     */
    static boolean isBlank(CharSequence str) {
        int length
        if (str == null || (length = str.length()) == 0) {
            return true
        }
        for (int i = 0; i < length; i++) {
            // 判断是否包含非空白字符
            if (!Character.isWhitespace(str.charAt(i))) {
                return false
            }
        }
        return true
    }

    /**
     * 字符串是否为非空白
     * @param str 字符串
     * @return 是否为非空白
     */
    static boolean isNotBlank(CharSequence str) {
        return !isBlank(str)
    }

    /**
     * 字符串是否为空
     * @param str 字符串
     * @return 是否为空
     */
    static boolean isEmpty(CharSequence str) {
        return str == null || str.length() == 0
    }

    /**
     * 字符串是否为非空
     * @param str 字符串
     * @return 是否为非空
     */
    static boolean isNotEmpty(CharSequence str) {
        return !isEmpty(str)
    }

    /**
     * 去除字符串首尾空白符
     * @param str 字符串
     * @return 去除空白后的字符串
     */
    static String trim(CharSequence str) {
        return str == null ? null : str.toString().trim()
    }

    /**
     * 截取分隔字符串之前的字符串，不包括分隔字符串
     * @param str 被查找的字符串
     * @param separator 分隔字符串（不包括）
     * @param isLastSeparator 是否查找最后一个分隔字符串（多次出现分隔字符串时选取最后一个），true为选取最后一个
     * @return 切割后的字符串
     */
    static String subBefore(CharSequence str, CharSequence separator, boolean isLastSeparator) {
        if (isEmpty(str) || separator == null) {
            return str == null ? null : str.toString()
        }

        final String strString = str.toString()
        final String separatorString = separator.toString()
        final int pos = isLastSeparator ? strString.lastIndexOf(separatorString) : strString.indexOf(separatorString)
        if (pos == -1) {
            return strString
        }
        return strString.substring(0, pos)
    }

    /**
     * 截取分隔字符串之后的字符串，不包括分隔字符串
     * @param str 被查找的字符串
     * @param separator 分隔字符串（不包括）
     * @param isLastSeparator 是否查找最后一个分隔字符串（多次出现分隔字符串时选取最后一个），true为选取最后一个
     * @return 切割后的字符串
     */
    static String subAfter(CharSequence str, CharSequence separator, boolean isLastSeparator) {
        if (isEmpty(str) || isEmpty(separator)) {
            return str == null ? null : str.toString()
        }

        final String strString = str.toString()
        final String separatorString = separator.toString()
        final int pos = isLastSeparator ? strString.lastIndexOf(separatorString) : strString.indexOf(separatorString)
        if (pos == -1) {
            return strString
        }
        return strString.substring(pos + separatorString.length())
    }

    /**
     * 格式化文本
     * @param template 文本模板，被替换的部分用 {} 表示
     * @param params 参数值
     * @return 格式化后的文本
     */
    static String format(CharSequence template, Object... params) {
        if (null == template) {
            return null
        }
        if (params == null || params.length == 0 || isBlank(template)) {
            return template.toString()
        }
        
        final String templateString = template.toString()
        final StringBuilder sb = new StringBuilder(templateString.length() + 50)
        int handledPosition = 0
        int delimiterIndex
        for (int argIndex = 0; argIndex < params.length; argIndex++) {
            delimiterIndex = templateString.indexOf("{}", handledPosition)
            if (delimiterIndex == -1) {
                if (handledPosition == 0) {
                    return templateString
                }
                // 字符串模板剩余部分不再包含占位符，加入剩余部分后返回结果
                sb.append(templateString.substring(handledPosition))
                return sb.toString()
            }
            
            // 转义符
            if (delimiterIndex > 0 && templateString.charAt(delimiterIndex - 1) == '\\') {
                if (delimiterIndex > 1 && templateString.charAt(delimiterIndex - 2) == '\\') {
                    // 双转义符，占位符依旧有效
                    sb.append(templateString.substring(handledPosition, delimiterIndex - 1))
                    sb.append(params[argIndex])
                    handledPosition = delimiterIndex + 2
                } else {
                    // 占位符被转义
                    argIndex--
                    sb.append(templateString.substring(handledPosition, delimiterIndex - 1))
                    sb.append('{')
                    handledPosition = delimiterIndex + 1
                }
            } else {
                // 正常占位符
                sb.append(templateString.substring(handledPosition, delimiterIndex))
                sb.append(params[argIndex])
                handledPosition = delimiterIndex + 2
            }
        }
        
        // 加入最后一个占位符后剩余的模板
        sb.append(templateString.substring(handledPosition))
        return sb.toString()
    }

    /**
     * 比较两个字符串（大小写敏感）
     * @param str1 字符串1
     * @param str2 字符串2
     * @return 如果两个字符串相同，或者都是null，则返回true
     */
    static boolean equals(CharSequence str1, CharSequence str2) {
        if (str1 == null) {
            return str2 == null
        }
        return str1.toString().equals(str2 == null ? null : str2.toString())
    }

    /**
     * 比较两个字符串（大小写不敏感）
     * @param str1 字符串1
     * @param str2 字符串2
     * @return 如果两个字符串相同，或者都是null，则返回true
     */
    static boolean equalsIgnoreCase(CharSequence str1, CharSequence str2) {
        if (str1 == null) {
            return str2 == null
        }
        return str1.toString().equalsIgnoreCase(str2 == null ? null : str2.toString())
    }

    /**
     * 给定字符串是否以任何一个字符串开始
     * @param str 给定字符串
     * @param prefixes 需要检测的开始字符串
     * @return 给定字符串是否以任何一个字符串开始
     */
    static boolean startWithAny(CharSequence str, CharSequence... prefixes) {
        if (isEmpty(str) || prefixes == null || prefixes.length == 0) {
            return false
        }

        for (CharSequence prefix : prefixes) {
            if (str.toString().startsWith(prefix.toString())) {
                return true
            }
        }
        return false
    }

    /**
     * 如果给定字符串为空，则返回默认字符串
     * @param str 字符串
     * @param defaultStr 默认字符串
     * @return 字符串本身或指定的默认字符串
     */
    static String emptyToDefault(CharSequence str, String defaultStr) {
        return isEmpty(str) ? defaultStr : str.toString()
    }

    /**
     * 如果给定字符串为空白，则返回默认字符串
     * @param str 字符串
     * @param defaultStr 默认字符串
     * @return 字符串本身或指定的默认字符串
     */
    static String blankToDefault(CharSequence str, String defaultStr) {
        return isBlank(str) ? defaultStr : str.toString()
    }
}
