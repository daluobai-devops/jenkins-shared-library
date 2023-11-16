package com.daluobai.jenkinslib.steps

import cn.hutool.core.date.DateUtil
@Grab('cn.hutool:hutool-all:5.8.11')

import cn.hutool.core.lang.Assert
import cn.hutool.core.util.ObjectUtil
import cn.hutool.core.util.StrUtil
import com.daluobai.jenkinslib.constant.GlobalShare
/**
 * @author daluobai@outlook.com
 * version 1.0.0
 * @title 
 * @description https://github.com/daluobai-devops/jenkins-shared-library
 * @create 2023/4/25 12:10
 */
class StepsJenkins implements Serializable {
    def steps

    StepsJenkins(steps) { this.steps = steps }

    /*******************初始化全局对象 开始*****************/
    def stepsGit = new StepsGit(steps)
    /*******************初始化全局对象 结束*****************/

    /**
     * 存储
     *
     */
    def stash(Map parameterMap) {
        Assert.notEmpty(parameterMap,"参数为空")
        def archiveType = parameterMap.archiveType
        def jenkinsStash = parameterMap.jenkinsStash
        def dockerRegistry = parameterMap.dockerRegistry
        def dockerfile = parameterMap.dockerRegistry.dockerfile
        def fullConfig = GlobalShare.globalParameterMap
        Assert.notBlank(archiveType,"archiveType为空")
        def includes
        def archiveName
        steps.sh "ls package -l"
        steps.sh "mkdir -p stash"

        //根据文件类型处理文件
        if (archiveType == "JAR") {
            archiveName = "app.jar"
            steps.sh "rm -f package/*-sources.jar"
            steps.sh "mv package/*.jar package/app.jar || true"
            includes = "package/app.jar"
        } else if (archiveType == "WAR") {
            archiveName = "app.war"
            steps.sh "mv package/*.war package/app.war || true"
            includes = "package/app.war"
        } else if (archiveType == "TAR") {
            archiveName = "app.tar.gz"
            steps.sh "mv package/*.tar.gz package/app.tar.gz || true"
            includes = "package/app.tar.gz"
        }else if (archiveType == "FOLDER"){
            archiveName = "app"
            includes = "package/app/**/*"
        }else {
            throw new Exception("archiveType不支持")
        }
        steps.sh "ls package -l"

        if (jenkinsStash.enable) {
            steps.stash name: "appPackage", includes: "${includes}"
        }
        if (dockerRegistry.enable) {
            steps.sh "mkdir -p stash/dockerRegistry/code/code"
            steps.dir("stash/dockerRegistry/code/code") {
                steps.git branch: "${dockerfile.gitBranch}", credentialsId: 'ssh-git', url: "${dockerfile.url}"
            }
//            steps.sh '''mv stash/dockerRegistry/code/\$(ls -A1 stash/dockerRegistry/code/) stash/dockerRegistry/code/code/'''
            //把构建的东西放到dockerfile目录下
            steps.sh "mkdir -p stash/dockerRegistry/code/code/${dockerfile.path}/build/package"
            steps.sh "cp -r ${includes} stash/dockerRegistry/code/code/${dockerfile.path}/build/package/"

            // 拼接
            def buildArgs = "--build-arg appName=${fullConfig.SHARE_PARAM.appName} "
            if (dockerRegistry.buildArgs != null && dockerRegistry.buildArgs.size() > 0) {
                dockerRegistry.buildArgs.each { key, value ->
                    if (StrUtil.isNotBlank(value) && StrUtil.isNotBlank(key)){
                        buildArgs += "--build-arg \'${key}\'=\'${value}\' "
                    }
                }
            }
            def imageName = StrUtil.isBlank(dockerRegistry.imageName) ? fullConfig.SHARE_PARAM.appName : dockerRegistry.imageName
            def imageVersion = StrUtil.isBlank(dockerRegistry.imageVersion) ? DateUtil.format(new Date(), "yyyyMMddHHmmss") : dockerRegistry.imageVersion
            steps.dir("stash/dockerRegistry/code/code/${dockerfile.path}") {
                steps.sh "ls -l"
                steps.sh "docker build ${buildArgs} -t ${dockerRegistry.imagePrefix}/${imageName}:${imageVersion} ."
                steps.sh "docker push ${dockerRegistry.imagePrefix}/${imageName}:${imageVersion}"
                archiveName = "${dockerRegistry.imagePrefix}/${imageName}:${imageVersion}"
            }
        }
        GlobalShare.globalParameterMap.SHARE_PARAM.put("archiveName",archiveName)
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
