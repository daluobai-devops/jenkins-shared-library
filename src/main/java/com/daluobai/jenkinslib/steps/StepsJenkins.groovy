package com.daluobai.jenkinslib.steps
@Grab('cn.hutool:hutool-all:5.8.11')

import cn.hutool.core.lang.Assert
import cn.hutool.core.util.StrUtil
import com.daluobai.jenkinslib.constant.GlobalShare
import com.daluobai.jenkinslib.utils.MapUtils

class StepsJenkins implements Serializable {
    def steps

    StepsJenkins(steps) { this.steps = steps }

    /**
     * 存储
     *
     */
    def stash(Map parameterMap) {
        Assert.notEmpty(parameterMap,"参数为空")
        def archiveType = parameterMap.archiveType
        def jenkinsStash = parameterMap.jenkinsStash
        def dockerRegistry = parameterMap.dockerRegistry
        Assert.notBlank(archiveType,"archiveType为空")
        def includes
        def archiveName
        steps.sh "ls package -l"
        //根据文件类型处理文件
        if (archiveType == "JAR") {
            archiveName = "app.jar"
            steps.sh "rm -f package/*-sources.jar"
            steps.sh "mv package/*.jar package/app.jar"
            includes = "package/app.jar"
        } else if (archiveType == "WAR") {
            archiveName = "app.war"
            includes = "package/app.war"
        } else if (archiveType == "ZIP") {
            archiveName = "app.zip"
            includes = "package/app.zip"
        } else {
            throw new Exception("archiveType不支持")
        }
        steps.sh "ls package -l"

        if (jenkinsStash.enable) {
            steps.stash name: "appPackage", includes: "${includes}"
        }
        if (dockerRegistry.enable) {

        }
        GlobalShare.globalParameterMap.put("archiveName",archiveName)
    }

    /**
     * 根据label获取节点，如果为空则返回主节点
     *
     */
    def getNodeByLabel(String label) {
        Assert.notBlank(label,"label为空")
        def nodeBuildNodeList = steps.nodesByLabel label: label
        if (nodeBuildNodeList != null && nodeBuildNodeList.size() > 0) {
            //这里因为如果是 master 节点返回的数组是空字符串，所以这里需要判断一下
            for (i in 0..< nodeBuildNodeList.size()) {
                if (StrUtil.isBlank(nodeBuildNodeList[i])){
                    nodeBuildNodeList[i] = "master"
                }
            }
        }

        return (nodeBuildNodeList).toList()
    }

}
