package com.daluobai.jenkinslib.utils

@Grab('cn.hutool:hutool-all:5.8.11')
import cn.hutool.json.JSONUtil
import cn.hutool.core.util.StrUtil
import cn.hutool.core.lang.Assert
import cn.hutool.core.io.FileUtil
import java.util.regex.Pattern;

class TemplateUtils implements Serializable {


    /**
     * GStringTemplateEngine
     * @param stringTemplateContent 模板
     * @param dataMap 数据map
     * @return
     */
    @Deprecated
    def stringTemplate(stringTemplateContent, dataMap) {
        Assert.notBlank(stringTemplateContent, "stringTemplateContent为空");
        Assert.notNull(dataMap, "dataMap为空");

        def engine = new groovy.text.GStringTemplateEngine()
        def template = engine.createTemplate(stringTemplateContent).make(dataMap)
        return template.toString()
    }

    /**
     * GStringTemplateEngine
     * @param stringTemplateContent 模板
     * @param dataMap 数据map
     * @return
     */
    static def makeTemplate(stringTemplateContent, dataMap) {
        Assert.notBlank(stringTemplateContent, "stringTemplateContent为空");
        Assert.notNull(dataMap, "dataMap为空");

        def engine = new groovy.text.GStringTemplateEngine()
        def template = engine.createTemplate(stringTemplateContent).make(dataMap)
        return template.toString()
    }
}