package com.daluobai.jenkinslib.utils

@Grab('cn.hutool:hutool-all:5.8.11')
import cn.hutool.core.lang.Assert
import cn.hutool.core.io.FileUtil
import cn.hutool.json.JSONObject
import cn.hutool.json.JSONUtil
import com.daluobai.jenkinslib.constant.EConfigType
import java.nio.charset.Charset

class ConfigUtils implements Serializable {

/**
 * 修改文件
 * @param path 文件路径
 * @return
 */
    static def readConfig(EConfigType eConfigType, String path) {
        Assert.notNull(eConfigType, "配置类型为空");
        Assert.notBlank(path, "path为空");
        JSONObject configJson = null
        if (eConfigType == EConfigType.HOST_PATH) {
            def file = new File(path)
            boolean isFile = FileUtil.isFile(file)
            Assert.isTrue(isFile, "配置文件不存在")
            configJson = JSONUtil.readJSON(file, Charset.forName("utf-8"))
        } else if (eConfigType == EConfigType.RESOURCES) {

        } else if (eConfigType == EConfigType.URL) {

        } else {
            throw new Exception("暂不支持的配置类型")
        }
        Assert.notNull(configJson, "配置文件读取失败");
        return configJson.toBean(Map)
    }

    static def readConfigFromResource(def steps, String path) {
        Assert.notNull(steps, "steps为空");
        Assert.notBlank(path, "path为空");
        def configFromResourceString = steps.libraryResource path
        def configFromResourceMap = MapUtils.mapString2Map(configFromResourceString)
        return configFromResourceMap
    }
}