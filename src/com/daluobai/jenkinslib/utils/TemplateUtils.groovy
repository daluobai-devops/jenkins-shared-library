package com.daluobai.jenkinslib.utils

import com.daluobai.jenkinslib.utils.AssertUtils
/**
 * @author daluobai@outlook.com
 * version 1.0.0
 * @title 
 * @description https://github.com/daluobai-devops/jenkins-shared-library
 * @create 2023/4/25 12:10
 */
class TemplateUtils implements Serializable {


    /**
     * GStringTemplateEngine
     * @param stringTemplateContent 模板
     * @param dataMap 数据map
     * @return
     */
    @Deprecated
    def stringTemplate(stringTemplateContent, dataMap) {
        AssertUtils.notBlank(stringTemplateContent, "stringTemplateContent为空");
        AssertUtils.notNull(dataMap, "dataMap为空");

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
        AssertUtils.notBlank(stringTemplateContent, "stringTemplateContent为空");
        AssertUtils.notNull(dataMap, "dataMap为空");

        def engine = new groovy.text.GStringTemplateEngine()
        def template = engine.createTemplate(stringTemplateContent).make(dataMap)
        return template.toString()
    }
}