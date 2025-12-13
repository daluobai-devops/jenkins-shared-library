package com.daluobai.jenkinslib.utils

import java.text.SimpleDateFormat
import java.text.ParseException

/**
 * @author daluobai@outlook.com
 * version 1.0.0
 * @title 日期工具类 - 参考hutool DateUtil
 * @description https://github.com/daluobai-devops/jenkins-shared-library
 * @create 2025/12/13
 */
class DateUtils implements Serializable {

    /**
     * 标准日期格式：yyyy-MM-dd
     */
    static final String NORM_DATE_PATTERN = "yyyy-MM-dd"
    
    /**
     * 标准时间格式：HH:mm:ss
     */
    static final String NORM_TIME_PATTERN = "HH:mm:ss"
    
    /**
     * 标准日期时间格式：yyyy-MM-dd HH:mm:ss
     */
    static final String NORM_DATETIME_PATTERN = "yyyy-MM-dd HH:mm:ss"
    
    /**
     * 标准日期时间格式（毫秒）：yyyy-MM-dd HH:mm:ss.SSS
     */
    static final String NORM_DATETIME_MS_PATTERN = "yyyy-MM-dd HH:mm:ss.SSS"
    
    /**
     * ISO8601日期格式：yyyy-MM-dd'T'HH:mm:ss
     */
    static final String ISO8601_PATTERN = "yyyy-MM-dd'T'HH:mm:ss"

    /**
     * 当前时间
     * @return Date对象
     */
    static Date now() {
        return new Date()
    }

    /**
     * 当前时间戳（毫秒）
     * @return 时间戳
     */
    static long currentTimeMillis() {
        return System.currentTimeMillis()
    }

    /**
     * 格式化日期
     * @param date 日期
     * @param format 格式
     * @return 格式化后的字符串
     */
    static String format(Date date, String format = NORM_DATETIME_PATTERN) {
        if (date == null) {
            return null
        }
        
        try {
            SimpleDateFormat sdf = new SimpleDateFormat(format)
            return sdf.format(date)
        } catch (Exception e) {
            throw new RuntimeException("日期格式化失败", e)
        }
    }

    /**
     * 格式化日期为标准日期格式
     * @param date 日期
     * @return yyyy-MM-dd
     */
    static String formatDate(Date date) {
        return format(date, NORM_DATE_PATTERN)
    }

    /**
     * 格式化日期为标准时间格式
     * @param date 日期
     * @return HH:mm:ss
     */
    static String formatTime(Date date) {
        return format(date, NORM_TIME_PATTERN)
    }

    /**
     * 格式化日期为标准日期时间格式
     * @param date 日期
     * @return yyyy-MM-dd HH:mm:ss
     */
    static String formatDateTime(Date date) {
        return format(date, NORM_DATETIME_PATTERN)
    }

    /**
     * 解析日期字符串
     * @param dateStr 日期字符串
     * @param format 格式
     * @return Date对象
     */
    static Date parse(String dateStr, String format = NORM_DATETIME_PATTERN) {
        if (StrUtils.isBlank(dateStr)) {
            return null
        }
        
        try {
            SimpleDateFormat sdf = new SimpleDateFormat(format)
            return sdf.parse(dateStr)
        } catch (ParseException e) {
            throw new RuntimeException("日期解析失败: " + dateStr, e)
        }
    }

    /**
     * 解析日期字符串（标准日期格式）
     * @param dateStr 日期字符串 yyyy-MM-dd
     * @return Date对象
     */
    static Date parseDate(String dateStr) {
        return parse(dateStr, NORM_DATE_PATTERN)
    }

    /**
     * 解析日期时间字符串（标准日期时间格式）
     * @param dateTimeStr 日期时间字符串 yyyy-MM-dd HH:mm:ss
     * @return Date对象
     */
    static Date parseDateTime(String dateTimeStr) {
        return parse(dateTimeStr, NORM_DATETIME_PATTERN)
    }

    /**
     * Date转时间戳
     * @param date 日期
     * @return 时间戳（毫秒）
     */
    static long toTimestamp(Date date) {
        return date == null ? 0 : date.getTime()
    }

    /**
     * 时间戳转Date
     * @param timestamp 时间戳（毫秒）
     * @return Date对象
     */
    static Date toDate(long timestamp) {
        return new Date(timestamp)
    }

    /**
     * 增加天数
     * @param date 日期
     * @param days 天数（可以为负数）
     * @return 新的Date对象
     */
    static Date addDays(Date date, int days) {
        if (date == null) {
            return null
        }
        
        Calendar calendar = Calendar.getInstance()
        calendar.setTime(date)
        calendar.add(Calendar.DAY_OF_MONTH, days)
        return calendar.getTime()
    }

    /**
     * 增加小时
     * @param date 日期
     * @param hours 小时数（可以为负数）
     * @return 新的Date对象
     */
    static Date addHours(Date date, int hours) {
        if (date == null) {
            return null
        }
        
        Calendar calendar = Calendar.getInstance()
        calendar.setTime(date)
        calendar.add(Calendar.HOUR_OF_DAY, hours)
        return calendar.getTime()
    }

    /**
     * 增加分钟
     * @param date 日期
     * @param minutes 分钟数（可以为负数）
     * @return 新的Date对象
     */
    static Date addMinutes(Date date, int minutes) {
        if (date == null) {
            return null
        }
        
        Calendar calendar = Calendar.getInstance()
        calendar.setTime(date)
        calendar.add(Calendar.MINUTE, minutes)
        return calendar.getTime()
    }

    /**
     * 增加秒数
     * @param date 日期
     * @param seconds 秒数（可以为负数）
     * @return 新的Date对象
     */
    static Date addSeconds(Date date, int seconds) {
        if (date == null) {
            return null
        }
        
        Calendar calendar = Calendar.getInstance()
        calendar.setTime(date)
        calendar.add(Calendar.SECOND, seconds)
        return calendar.getTime()
    }

    /**
     * 计算两个日期之间的天数差
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 天数差（endDate - startDate）
     */
    static long betweenDays(Date startDate, Date endDate) {
        if (startDate == null || endDate == null) {
            return 0
        }
        
        long diff = endDate.getTime() - startDate.getTime()
        return diff / (1000 * 60 * 60 * 24)
    }

    /**
     * 计算两个日期之间的小时差
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 小时差（endDate - startDate）
     */
    static long betweenHours(Date startDate, Date endDate) {
        if (startDate == null || endDate == null) {
            return 0
        }
        
        long diff = endDate.getTime() - startDate.getTime()
        return diff / (1000 * 60 * 60)
    }

    /**
     * 计算两个日期之间的分钟差
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 分钟差（endDate - startDate）
     */
    static long betweenMinutes(Date startDate, Date endDate) {
        if (startDate == null || endDate == null) {
            return 0
        }
        
        long diff = endDate.getTime() - startDate.getTime()
        return diff / (1000 * 60)
    }

    /**
     * 计算两个日期之间的秒数差
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 秒数差（endDate - startDate）
     */
    static long betweenSeconds(Date startDate, Date endDate) {
        if (startDate == null || endDate == null) {
            return 0
        }
        
        long diff = endDate.getTime() - startDate.getTime()
        return diff / 1000
    }

    /**
     * 计算两个日期之间的毫秒差
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 毫秒差（endDate - startDate）
     */
    static long betweenMillis(Date startDate, Date endDate) {
        if (startDate == null || endDate == null) {
            return 0
        }
        
        return endDate.getTime() - startDate.getTime()
    }

    /**
     * 判断是否为同一天
     * @param date1 日期1
     * @param date2 日期2
     * @return 是否为同一天
     */
    static boolean isSameDay(Date date1, Date date2) {
        if (date1 == null || date2 == null) {
            return false
        }
        
        Calendar cal1 = Calendar.getInstance()
        cal1.setTime(date1)
        
        Calendar cal2 = Calendar.getInstance()
        cal2.setTime(date2)
        
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
               cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH) &&
               cal1.get(Calendar.DAY_OF_MONTH) == cal2.get(Calendar.DAY_OF_MONTH)
    }

    /**
     * 获取年份
     * @param date 日期
     * @return 年份
     */
    static int getYear(Date date) {
        if (date == null) {
            return 0
        }
        
        Calendar calendar = Calendar.getInstance()
        calendar.setTime(date)
        return calendar.get(Calendar.YEAR)
    }

    /**
     * 获取月份（1-12）
     * @param date 日期
     * @return 月份
     */
    static int getMonth(Date date) {
        if (date == null) {
            return 0
        }
        
        Calendar calendar = Calendar.getInstance()
        calendar.setTime(date)
        return calendar.get(Calendar.MONTH) + 1
    }

    /**
     * 获取日（1-31）
     * @param date 日期
     * @return 日
     */
    static int getDayOfMonth(Date date) {
        if (date == null) {
            return 0
        }
        
        Calendar calendar = Calendar.getInstance()
        calendar.setTime(date)
        return calendar.get(Calendar.DAY_OF_MONTH)
    }

    /**
     * 获取小时（0-23）
     * @param date 日期
     * @return 小时
     */
    static int getHour(Date date) {
        if (date == null) {
            return 0
        }
        
        Calendar calendar = Calendar.getInstance()
        calendar.setTime(date)
        return calendar.get(Calendar.HOUR_OF_DAY)
    }

    /**
     * 获取分钟（0-59）
     * @param date 日期
     * @return 分钟
     */
    static int getMinute(Date date) {
        if (date == null) {
            return 0
        }
        
        Calendar calendar = Calendar.getInstance()
        calendar.setTime(date)
        return calendar.get(Calendar.MINUTE)
    }

    /**
     * 获取秒（0-59）
     * @param date 日期
     * @return 秒
     */
    static int getSecond(Date date) {
        if (date == null) {
            return 0
        }
        
        Calendar calendar = Calendar.getInstance()
        calendar.setTime(date)
        return calendar.get(Calendar.SECOND)
    }

    /**
     * 获取当天开始时间（00:00:00）
     * @param date 日期
     * @return 当天开始时间
     */
    static Date beginOfDay(Date date) {
        if (date == null) {
            return null
        }
        
        Calendar calendar = Calendar.getInstance()
        calendar.setTime(date)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.getTime()
    }

    /**
     * 获取当天结束时间（23:59:59）
     * @param date 日期
     * @return 当天结束时间
     */
    static Date endOfDay(Date date) {
        if (date == null) {
            return null
        }
        
        Calendar calendar = Calendar.getInstance()
        calendar.setTime(date)
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        return calendar.getTime()
    }
}
