package com.daluobai.jenkinslib.steps
@Grab('cn.hutool:hutool-all:5.8.11')

import cn.hutool.core.lang.Assert
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
        Assert.notBlank(archiveType,"archiveType为空")
        def includes
        def archiveName
        steps.sh "ls package -l"
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

            steps.git credentialsId: 'ssh-git', url: "${ENV_DOCKER_BUILD_APP_IMAGE_GIT_URL}"
            sh "mkdir -p build"
            sh "cp ${ENV_DOCKER_BUILD_PACKAGE_BASE_PATH}/${ENV_DOCKER_BUILD_ID}/app.jar ./build/"
            sh "docker build \
                        --build-arg PARAM_JAVA_ARGS='${PARAM_JAVA_ARGS}' \
                        --build-arg PARAM_JAVA_OPTS='${_TEMP_DOCKER_JAVA_OPTS_PARAM}' \
                        -t=${_TEMP_DOCKER_BUILD_APP_IMAGE_FULL_NAME} \
                        ."
            sh "docker push ${_TEMP_DOCKER_BUILD_APP_IMAGE_FULL_NAME}"

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
