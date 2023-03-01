package com.daluobai.jenkinslib.utils

import cn.hutool.core.io.FileUtil
@Grab('cn.hutool:hutool-all:5.8.11')
import cn.hutool.core.lang.Assert
import cn.hutool.core.util.StrUtil
import cn.hutool.http.HttpUtil
import com.daluobai.jenkinslib.constant.EFileReadType

import java.nio.charset.Charset

class FileUtils implements Serializable {

    def steps

    FileUtils(steps) { this.steps = steps }

    /**
     * 从完整路径读取文件
     * @param fileFullPath
     * @return
     */
    def readStringFromFullPath(String fileFullPath) {
        Assert.notBlank(fileFullPath, "fileFullPath为空");
        def configType = StrUtil.subBefore(fileFullPath, ":", false)
        //获取后缀
        def path = StrUtil.subAfter(fileFullPath, ":", false)
        EFileReadType extendConfigType = EFileReadType.get(configType)
        return this.readString(extendConfigType, path)
    }

/**
 * 修改文件
 * @param path 文件路径
 * @return
 */
    def readString(EFileReadType eConfigType, String path) {
        Assert.notNull(eConfigType, "配置类型为空");
        Assert.notBlank(path, "path为空")
        def fileString = ""
        if (eConfigType == EFileReadType.HOST_PATH) {
            def file = new File(path)
            boolean isFile = FileUtil.isFile(file)
            Assert.isTrue(isFile, "配置文件不存在")
            fileString = FileUtil.readString(file, Charset.forName("utf-8"))
        } else if (eConfigType == EFileReadType.RESOURCES) {
            fileString = steps.libraryResource path
        } else if (eConfigType == EFileReadType.URL) {
            fileString = HttpUtil.get(path)
        } else {
            throw new Exception("暂不支持的配置类型")
        }
        return fileString
    }
}