package com.daluobai.jenkinslib.utils

@Grab('cn.hutool:hutool-all:5.8.11')
import cn.hutool.core.lang.Assert
import cn.hutool.core.io.FileUtil
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
class ConfigUtils implements Serializable {

    def steps

    ConfigUtils(steps) { this.steps = steps }

    /**
     * 从完整路径读取配置
     * @param configFullPath
     * @return
     */
    def readConfigFromFullPath(String configFullPath) {
        Assert.notBlank(configFullPath, "configFullPath为空");
        def configType = StrUtil.subBefore(configFullPath, ":", false)
        //获取后缀
        def path = StrUtil.subAfter(configFullPath, ":", false)
        EFileReadType extendConfigType = EFileReadType.get(configType)
        return this.readConfig(extendConfigType, path)
    }

/**
 * 修改文件
 * @param path 文件路径
 * @return
 */
    def readConfig(EFileReadType eConfigType, String path) {
        Assert.notNull(eConfigType, "配置类型为空");
        Assert.notBlank(path, "path为空");
        def configMap = [:]
        def configStr = new FileUtils(steps).readString(eConfigType, path)
        if (eConfigType == EFileReadType.HOST_PATH) {
            def file = new File(path)
            boolean isFile = FileUtil.isFile(file)
            Assert.isTrue(isFile, "配置文件不存在")
            configMap = MapUtils.mapJsonString2Map(configStr)
        } else if (eConfigType == EFileReadType.RESOURCES) {
            configMap = MapUtils.mapJsonString2Map(configStr)
        } else if (eConfigType == EFileReadType.URL) {
            configMap = MapUtils.mapJsonString2Map(configStr)
        } else {
            throw new Exception("暂不支持的配置类型")
        }
        Assert.notNull(configMap, "配置文件读取失败");
        return configMap
    }
}