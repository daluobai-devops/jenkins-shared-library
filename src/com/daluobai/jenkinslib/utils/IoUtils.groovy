package com.daluobai.jenkinslib.utils

import java.nio.charset.Charset

/**
 * @author daluobai@outlook.com
 * version 1.0.0
 * @title 文件工具类 - 参考hutool FileUtil
 * @description https://github.com/daluobai-devops/jenkins-shared-library
 * @create 2025/12/13
 */
class IoUtils implements Serializable {

    /**
     * 判断是否为文件
     * @param file 文件
     * @return 如果为文件，则返回true
     */
    static boolean isFile(File file) {
        return file != null && file.exists() && file.isFile()
    }

    /**
     * 判断是否为目录
     * @param file 文件
     * @return 如果为目录，则返回true
     */
    static boolean isDirectory(File file) {
        return file != null && file.exists() && file.isDirectory()
    }

    /**
     * 判断文件是否存在
     * @param file 文件
     * @return 如果存在，则返回true
     */
    static boolean exists(File file) {
        return file != null && file.exists()
    }

    /**
     * 读取文件内容为字符串
     * @param file 文件
     * @param charset 字符集
     * @return 文件内容
     */
    static String readString(File file, Charset charset = Charset.forName("UTF-8")) {
        if (file == null || !file.exists()) {
            return null
        }
        
        try {
            return file.getText(charset.name())
        } catch (IOException e) {
            throw new RuntimeException("读取文件失败: " + file.getAbsolutePath(), e)
        }
    }

    /**
     * 读取文件内容为字符串
     * @param filePath 文件路径
     * @param charset 字符集
     * @return 文件内容
     */
    static String readString(String filePath, Charset charset = Charset.forName("UTF-8")) {
        if (StrUtils.isBlank(filePath)) {
            return null
        }
        return readString(new File(filePath), charset)
    }

    /**
     * 写入字符串到文件
     * @param content 内容
     * @param file 文件
     * @param charset 字符集
     */
    static void writeString(String content, File file, Charset charset = Charset.forName("UTF-8")) {
        if (file == null) {
            throw new IllegalArgumentException("文件不能为null")
        }
        
        try {
            // 确保父目录存在
            File parent = file.getParentFile()
            if (parent != null && !parent.exists()) {
                parent.mkdirs()
            }
            
            file.write(content, charset.name())
        } catch (IOException e) {
            throw new RuntimeException("写入文件失败: " + file.getAbsolutePath(), e)
        }
    }

    /**
     * 写入字符串到文件
     * @param content 内容
     * @param filePath 文件路径
     * @param charset 字符集
     */
    static void writeString(String content, String filePath, Charset charset = Charset.forName("UTF-8")) {
        if (StrUtils.isBlank(filePath)) {
            throw new IllegalArgumentException("文件路径不能为空")
        }
        writeString(content, new File(filePath), charset)
    }

    /**
     * 追加字符串到文件
     * @param content 内容
     * @param file 文件
     * @param charset 字符集
     */
    static void appendString(String content, File file, Charset charset = Charset.forName("UTF-8")) {
        if (file == null) {
            throw new IllegalArgumentException("文件不能为null")
        }
        
        try {
            // 确保父目录存在
            File parent = file.getParentFile()
            if (parent != null && !parent.exists()) {
                parent.mkdirs()
            }
            
            file.append(content, charset.name())
        } catch (IOException e) {
            throw new RuntimeException("追加文件失败: " + file.getAbsolutePath(), e)
        }
    }

    /**
     * 读取文件的所有行
     * @param file 文件
     * @param charset 字符集
     * @return 行列表
     */
    static List<String> readLines(File file, Charset charset = Charset.forName("UTF-8")) {
        if (file == null || !file.exists()) {
            return []
        }
        
        try {
            return file.readLines(charset.name())
        } catch (IOException e) {
            throw new RuntimeException("读取文件失败: " + file.getAbsolutePath(), e)
        }
    }

    /**
     * 删除文件或目录
     * @param file 文件或目录
     * @return 是否删除成功
     */
    static boolean delete(File file) {
        if (file == null || !file.exists()) {
            return true
        }
        
        if (file.isDirectory()) {
            // 递归删除目录
            File[] files = file.listFiles()
            if (files != null) {
                for (File f : files) {
                    delete(f)
                }
            }
        }
        
        return file.delete()
    }

    /**
     * 创建文件及其父目录
     * @param file 文件
     * @return 是否创建成功
     */
    static boolean createNewFile(File file) {
        if (file == null) {
            return false
        }
        
        if (file.exists()) {
            return true
        }
        
        try {
            // 确保父目录存在
            File parent = file.getParentFile()
            if (parent != null && !parent.exists()) {
                parent.mkdirs()
            }
            
            return file.createNewFile()
        } catch (IOException e) {
            throw new RuntimeException("创建文件失败: " + file.getAbsolutePath(), e)
        }
    }

    /**
     * 创建目录
     * @param dir 目录
     * @return 是否创建成功
     */
    static boolean mkdir(File dir) {
        if (dir == null) {
            return false
        }
        
        if (dir.exists()) {
            return dir.isDirectory()
        }
        
        return dir.mkdirs()
    }

    /**
     * 复制文件
     * @param src 源文件
     * @param dest 目标文件
     * @param isOverride 是否覆盖已存在的文件
     */
    static void copy(File src, File dest, boolean isOverride = true) {
        if (src == null || !src.exists()) {
            throw new IllegalArgumentException("源文件不存在")
        }
        
        if (dest == null) {
            throw new IllegalArgumentException("目标文件不能为null")
        }
        
        if (dest.exists() && !isOverride) {
            throw new IllegalArgumentException("目标文件已存在")
        }
        
        try {
            // 确保目标目录存在
            File parent = dest.getParentFile()
            if (parent != null && !parent.exists()) {
                parent.mkdirs()
            }
            
            dest.withOutputStream { os ->
                src.withInputStream { is ->
                    os << is
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("复制文件失败", e)
        }
    }

    /**
     * 获取文件扩展名
     * @param file 文件
     * @return 扩展名（不包含点）
     */
    static String getExtension(File file) {
        if (file == null) {
            return null
        }
        return getExtension(file.getName())
    }

    /**
     * 获取文件扩展名
     * @param fileName 文件名
     * @return 扩展名（不包含点）
     */
    static String getExtension(String fileName) {
        if (StrUtils.isBlank(fileName)) {
            return null
        }
        
        int index = fileName.lastIndexOf('.')
        if (index == -1) {
            return ""
        }
        
        return fileName.substring(index + 1)
    }

    /**
     * 获取文件名（不包含扩展名）
     * @param file 文件
     * @return 文件名
     */
    static String getNameWithoutExtension(File file) {
        if (file == null) {
            return null
        }
        return getNameWithoutExtension(file.getName())
    }

    /**
     * 获取文件名（不包含扩展名）
     * @param fileName 文件名
     * @return 文件名
     */
    static String getNameWithoutExtension(String fileName) {
        if (StrUtils.isBlank(fileName)) {
            return null
        }
        
        int index = fileName.lastIndexOf('.')
        if (index == -1) {
            return fileName
        }
        
        return fileName.substring(0, index)
    }
}
