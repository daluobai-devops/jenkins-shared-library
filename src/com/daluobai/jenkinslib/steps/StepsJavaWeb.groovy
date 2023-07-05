package com.daluobai.jenkinslib.steps

import cn.hutool.core.date.DateUtil
import cn.hutool.core.io.FileUtil

//@Grab('cn.hutool:hutool-all:5.8.11')

import cn.hutool.core.lang.Assert
import cn.hutool.core.util.ObjectUtil
import com.daluobai.jenkinslib.constant.GlobalShare
import com.daluobai.jenkinslib.utils.TemplateUtils
import cn.hutool.core.util.StrUtil/**
 * @author daluobai@outlook.com
 * version 1.0.0
 * @title 
 * @description https://github.com/daluobai-devops/jenkins-shared-library
 * @create 2023/4/25 12:10
 */
class StepsJavaWeb implements Serializable {
    def steps

    StepsJavaWeb(steps) { this.steps = steps }

    /*******************初始化全局对象 开始*****************/
    def stepsJenkins = new StepsJenkins(steps)
    /*******************初始化全局对象 结束*****************/

    //发布
    def deploy(Map parameterMap) {
        steps.echo "StepsJavaWeb:${parameterMap}"
        Assert.notEmpty(parameterMap,"参数为空")
        def labels = parameterMap.labels
        def pathRoot = parameterMap.pathRoot
        def appName = GlobalShare.globalParameterMap.SHARE_PARAM.appName
        def archiveName = GlobalShare.globalParameterMap.SHARE_PARAM.archiveName
        //获取文件名后缀
        def archiveSuffix = StrUtil.subAfter(archiveName, ".", true)
        Assert.notEmpty(labels,"labels为空")

        def backAppName = "app-" + DateUtil.format(new Date(), "yyyyMMddHHmmss") + "." + archiveSuffix
//        steps.withCredentials([steps.sshUserPrivateKey(credentialsId: 'ssh-jenkins', keyFileVariable: 'SSH_KEY_PATH')]) {
//            steps.sh "mkdir -p ~/.ssh && chmod 700 ~/.ssh && rm -f ~/.ssh/id_rsa && cp \${SSH_KEY_PATH} ~/.ssh/id_rsa && chmod 600 ~/.ssh/id_rsa"
//        }
        labels.each{ c ->
            def label = c
            steps.echo "发布第一个标签:${label}"
            def nodeDeployNodeList = stepsJenkins.getNodeByLabel(label)
            steps.echo "获取到发布节点:${nodeDeployNodeList}"
            if (ObjectUtil.isEmpty(nodeDeployNodeList)) {
                steps.error '没有可用的发布节点'
            }
            nodeDeployNodeList.each{ d ->
                def nodeDeployNode = d
                steps.echo "开始发布:${nodeDeployNode}"
                steps.node(nodeDeployNode) {
                    steps.unstash("appPackage")
                    steps.sh "hostname"
                    steps.sh "ls -l package"
                    steps.sh "mkdir -p ${pathRoot}/${appName} && mkdir -p ${pathRoot}/${appName}/backup"
                    //备份
                    steps.sh "mv ${pathRoot}/${appName}/${archiveName} ${pathRoot}/${appName}/backup/${backAppName} || true"
                    steps.dir("${pathRoot}/${appName}/backup/"){
                        steps.sh "find . -mtime +3 -delete"
                    }
                    //拷贝新的包到发布目录
                    steps.sh "cp package/${archiveName} ${pathRoot}/${appName}"
                    steps.echo "开始===systemctlRe"
                    //判断是否有systemctl命令
                    def systemctlRe = steps.sh returnStatus: true, script: 'command -v systemctl'
                    steps.echo "结束===systemctlRe:${systemctlRe}"
                    if (systemctlRe == 0) {
                        steps.echo "通过systemctl重启"
                        //systemctl重启
                        reStartBySystemctl(parameterMap)
                    } else {
                        steps.echo "通过shell重启"
                        reStartByShell(parameterMap)
                    }
                }
            }
        }
    }

    /**
     * Systemctl重启
     * @param parameterMap
     * @return
     */
    def reStartBySystemctl(Map parameterMap){
        Assert.notEmpty(parameterMap,"参数为空")
        def appName = GlobalShare.globalParameterMap.SHARE_PARAM.appName
        def archiveName = GlobalShare.globalParameterMap.SHARE_PARAM.archiveName
        def labels = parameterMap.labels
        def pathRoot = parameterMap.pathRoot
        //生成服务文件
        steps.sh "systemctl stop ${appName}.service || true"
        steps.sh "rm -f /etc/systemd/system/${appName}.service || true"
        def serviceTemplate = steps.libraryResource 'template/service/JavaWeb.service'
        def templateData = [
                runOptions: parameterMap.runOptions,
                pathRoot: parameterMap.pathRoot,
                appName: appName,
                archiveName: archiveName,
                runArgs: parameterMap.runArgs
        ]
        steps.writeFile file: "/etc/systemd/system/${appName}.service", text: TemplateUtils.makeTemplate(serviceTemplate,templateData)
        steps.sh "systemctl daemon-reload"
        //切换到发布目录
        steps.dir("${pathRoot}/${appName}"){
            steps.sh "systemctl enable ${appName}.service"
            steps.sh "systemctl start ${appName}.service"
        }
    }

    def reStartByShell(Map parameterMap){
        Assert.notEmpty(parameterMap,"参数为空")
        def appName = GlobalShare.globalParameterMap.SHARE_PARAM.appName
        def archiveName = GlobalShare.globalParameterMap.SHARE_PARAM.archiveName
        def labels = parameterMap.labels
        def pathRoot = parameterMap.pathRoot
        def shellPath = "${pathRoot}/${appName}/service.sh"
        if (!FileUtil.exist(shellPath)){
            //生成脚本文件
            def serviceTemplate = steps.libraryResource 'template/shell/javaWeb/service.sh'
            def templateData = [
                    runOptions: parameterMap.runOptions,
                    pathRoot: parameterMap.pathRoot,
                    appName: appName,
                    archiveName: archiveName,
                    runArgs: parameterMap.runArgs
            ]
            steps.writeFile file: "${shellPath}", text: TemplateUtils.makeTemplate(serviceTemplate,templateData)
            steps.sh "chmod +x ${shellPath}"
        }
        steps.withEnv(["JENKINS_NODE_COOKIE=dontKillMe"]) {
            steps.sh "sh $shellPath restart"
        }
    }

}
