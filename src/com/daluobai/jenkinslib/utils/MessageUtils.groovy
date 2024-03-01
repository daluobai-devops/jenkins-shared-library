package com.daluobai.jenkinslib.utils

import cn.hutool.core.io.FileUtil
@Grab('cn.hutool:hutool-all:5.8.11')
import cn.hutool.core.lang.Assert
import cn.hutool.core.util.StrUtil
import cn.hutool.http.HttpUtil
import com.daluobai.jenkinslib.constant.EFileReadType

import java.nio.charset.Charset
/**
 * @author daluobai@outlook.com
 * version 1.0.0
 * @title 
 * @description https://github.com/daluobai-devops/jenkins-shared-library
 * @create 2023/4/25 12:10
 */
class MessageUtils implements Serializable {

    def steps

    MessageUtils(steps) { this.steps = steps }

    /**
     * 从完整路径读取文件
     * @param fileFullPath
     * @return
     */
    def sendMessage(messageConfig) {
        Assert.notEmpty(messageConfig, "messageConfig为空");
        def configType = StrUtil.subBefore(fileFullPath, ":", false)
        //获取后缀
        def path = StrUtil.subAfter(fileFullPath, ":", false)
        EFileReadType extendConfigType = EFileReadType.get(configType)
        return this.readString(extendConfigType, path)
    }

}