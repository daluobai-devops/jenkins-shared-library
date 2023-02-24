package com.daluobai.jenkinslib.utils

@Grab('cn.hutool:hutool-all:5.8.11')
import cn.hutool.core.lang.Assert
import cn.hutool.core.io.FileUtil
import cn.hutool.http.HttpUtil
import cn.hutool.json.JSONObject
import cn.hutool.json.JSONUtil
import com.daluobai.jenkinslib.constant.EConfigType
import java.nio.charset.Charset

class ConfigUtils implements Serializable {

    def steps

    ConfigUtils(steps) { this.steps = steps }

/**
 * 修改文件
 * @param path 文件路径
 * @return
 */
    def readConfig(EConfigType eConfigType, String path) {
        Assert.notNull(eConfigType, "配置类型为空");
        Assert.notBlank(path, "path为空");
        def configMap = [:]
        if (eConfigType == EConfigType.HOST_PATH) {
            def file = new File(path)
            boolean isFile = FileUtil.isFile(file)
            Assert.isTrue(isFile, "配置文件不存在")
            def configStr = FileUtil.readString(file, Charset.forName("utf-8"))
            configMap = MapUtils.mapString2Map(configStr)
        } else if (eConfigType == EConfigType.RESOURCES) {
            def configFromResourceString = steps.libraryResource path
            configMap = MapUtils.mapString2Map(configFromResourceString)
        } else if (eConfigType == EConfigType.URL) {
            def configStr = HttpUtil.get(path)
            configMap = MapUtils.mapString2Map(configStr)
        } else {
            throw new Exception("暂不支持的配置类型")
        }
        Assert.notNull(configMap, "配置文件读取失败");
        return configMap
    }

    static def readConfigFromResource(def steps, String path) {
        Assert.notNull(steps, "steps为空");
        Assert.notBlank(path, "path为空");
        def configFromResourceString = steps.libraryResource path
        def configFromResourceMap = MapUtils.mapString2Map(configFromResourceString)
        return configFromResourceMap
    }
}