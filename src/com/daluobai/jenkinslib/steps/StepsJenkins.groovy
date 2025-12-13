package com.daluobai.jenkinslib.steps

import com.daluobai.jenkinslib.utils.DateUtils
import com.daluobai.jenkinslib.utils.AssertUtils
import com.daluobai.jenkinslib.utils.ObjUtils
import com.daluobai.jenkinslib.utils.StrUtils
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
        AssertUtils.notEmpty(parameterMap,"参数为空")
        def archiveType = parameterMap.archiveType
        def jenkinsStash = parameterMap.jenkinsStash
        def archiveArtifacts = parameterMap.archiveArtifacts
        def dockerRegistry = parameterMap.dockerRegistry
        def dockerfile = parameterMap?.dockerRegistry?.dockerfile
        def fullConfig = steps.globalParameterMap
        AssertUtils.notBlank(archiveType,"archiveType为空")
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
        }
        else if (archiveType == "ZIP") {
            archiveName = "app.zip"
            steps.sh "mv package/*.zip package/app.zip || true"
            includes = "package/app.zip"
        }
//        else if (archiveType == "FOLDER"){
//            archiveName = "app"
//            includes = "package/app/**/*"
//        }
        else {
            throw new Exception("archiveType不支持")
        }
        steps.sh "ls package -l"

        if (archiveArtifacts == true){
            def jobName = steps.currentBuild.projectName
            //获取archiveName的后缀
            def archiveSuffix = archiveName.substring(archiveName.indexOf(".") + 1);
            def archiveArtifactName = "${steps.currentBuild.projectName}-${DateUtils.format(new Date(), "yyyyMMddHHmmss")}.${archiveSuffix}"
            steps.sh "\\cp -f ${includes} package/${archiveArtifactName} || true"
            steps.archiveArtifacts artifacts: "package/${archiveArtifactName}", followSymlinks: false
        }

        if (jenkinsStash?.enable) {
            steps.stash name: "appPackage", includes: "${includes}"
        }
        if (dockerRegistry?.enable) {
            steps.sh "mkdir -p stash/dockerRegistry/code/code"
            steps.dir("stash/dockerRegistry/code/code") {
                steps.git branch: "${dockerfile.gitBranch}", credentialsId: 'ssh-git', url: "${dockerfile.url}"
            }
//            steps.sh '''mv stash/dockerRegistry/code/\$(ls -A1 stash/dockerRegistry/code/) stash/dockerRegistry/code/code/'''
            //把构建的东西放到dockerfile目录下
            steps.sh "mkdir -p stash/dockerRegistry/code/code/${dockerfile.path}/build/package"
            steps.sh "cp -r ${includes} stash/dockerRegistry/code/code/${dockerfile.path}/build/package/"

            // 拼接
            def buildArgs = ""
            if (dockerRegistry.buildArgs != null && dockerRegistry.buildArgs.size() > 0) {
                dockerRegistry.buildArgs.each { key, value ->
                    if (StrUtils.isNotBlank(value) && StrUtils.isNotBlank(key)){
                        buildArgs += "--build-arg \'${key}\'=\'${value}\' "
                    }
                }
            }
            def imageName = StrUtils.isBlank(dockerRegistry.imageName) ? fullConfig.SHARE_PARAM.appName : dockerRegistry.imageName
            def imageVersion = StrUtils.isBlank(dockerRegistry.imageVersion) ? DateUtils.format(new Date(), "yyyyMMddHHmmss") : dockerRegistry.imageVersion
            steps.dir("stash/dockerRegistry/code/code/${dockerfile.path}") {
                steps.sh "ls -l"
                steps.sh "docker build ${buildArgs} -t ${dockerRegistry.imagePrefix}/${imageName}:${imageVersion} ."
                steps.sh "docker push ${dockerRegistry.imagePrefix}/${imageName}:${imageVersion}"
                archiveName = "${dockerRegistry.imagePrefix}/${imageName}:${imageVersion}"
            }
        }
        fullConfig.SHARE_PARAM.put("archiveName",archiveName)
    }

    /**
     * 根据label获取节点，如果为空则返回主节点
     *
     */
    def getNodeByLabel(String label) {
        AssertUtils.notBlank(label,"label为空")
        def nodeBuildNodeList = steps.nodesByLabel label: label
        if (nodeBuildNodeList != null && nodeBuildNodeList.size() > 0) {
            //这里因为如果是 master 节点返回的数组是空字符串，所以这里需要判断一下
            for (i in 0..< nodeBuildNodeList.size()) {
                if (StrUtils.isBlank(nodeBuildNodeList[i])){
                    nodeBuildNodeList[i] = "master"
                }
            }
        }

        return (nodeBuildNodeList).toList()
    }

}
