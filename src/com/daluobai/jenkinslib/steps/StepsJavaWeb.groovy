package com.daluobai.jenkinslib.steps

import cn.hutool.core.date.DateUtil
import cn.hutool.core.io.FileUtil

//@Grab('cn.hutool:hutool-all:5.8.11')

import cn.hutool.core.lang.Assert
import cn.hutool.core.util.ObjectUtil
import com.daluobai.jenkinslib.constant.GlobalShare
import com.daluobai.jenkinslib.utils.EndpointUtils
import com.daluobai.jenkinslib.utils.JenkinsUtils
import com.daluobai.jenkinslib.utils.TemplateUtils
import cn.hutool.core.util.StrUtil
/**
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
    def endpointUtils = new EndpointUtils(steps)
    def jenkinsUtils = new JenkinsUtils(steps)
    /*******************初始化全局对象 结束*****************/

    //发布
    def deploy(Map parameterMap) {
        steps.echo "StepsJavaWeb:${parameterMap}"
        Assert.notEmpty(parameterMap,"参数为空")
        def pathRoot = parameterMap.pathRoot
        def appName = GlobalShare.globalParameterMap.SHARE_PARAM.appName
        def archiveName = GlobalShare.globalParameterMap.SHARE_PARAM.archiveName
        //获取文件名后缀
        def archiveSuffix = StrUtil.subAfter(archiveName, ".", true)

        def backAppName = "app-" + DateUtil.format(new Date(), "yyyyMMddHHmmss") + "." + archiveSuffix
//        steps.withCredentials([steps.sshUserPrivateKey(credentialsId: 'ssh-jenkins', keyFileVariable: 'SSH_KEY_PATH')]) {
//            steps.sh "mkdir -p ~/.ssh && chmod 700 ~/.ssh && rm -f ~/.ssh/id_rsa && cp \${SSH_KEY_PATH} ~/.ssh/id_rsa && chmod 600 ~/.ssh/id_rsa"
//        }
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
        steps.sh "\\cp -f package/${archiveName} ${pathRoot}/${appName}"
        if (parameterMap.manageBy){

        }
        //判断用哪种方式管理服务
        def manageBySystemctl = StrUtil.isBlank(parameterMap.manageBy) || parameterMap.manageBy == "systemctl"
        //判断是否有systemctl命令，返回0表示有，返回1表示没有
        def systemctlRe = steps.sh returnStatus: true, script: 'command -v systemctl'
        if (manageBySystemctl && systemctlRe == 0) {
            steps.echo "通过systemctl重启"
            //systemctl重启
            reStartBySystemctl(parameterMap)
        } else {
            steps.echo "通过shell重启"
            reStartByShell(parameterMap)
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
        def javaPath = ObjectUtil.isEmpty(parameterMap.javaPath) ? "/usr/local/bin/java" : parameterMap.javaPath
        //获取登录的用户
        def loginUser= jenkinsUtils.pipelineSH("whoami")
        steps.echo "当前登录用户:${loginUser}"

        //生成服务文件
        steps.sh "systemctl stop ${appName}.service || true"
        steps.sh "rm -f /etc/systemd/systemO/${appName}.service || true"
        def serviceTemplate = steps.libraryResource 'template/service/JavaWeb.service'
        def templateData = [
                javaPath: javaPath,
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
        def javaPath = ObjectUtil.isEmpty(parameterMap.javaPath) ? "/usr/local/bin/java" : parameterMap.javaPath
        def shellPath = "${pathRoot}/${appName}/service.sh"
        //先删掉原来的脚本文件
        steps.sh "rm -f ${shellPath} || true"
        //生成脚本文件
        def serviceTemplate = steps.libraryResource 'template/shell/javaWeb/service.sh'
        def templateData = [
                javaPath: javaPath,
                runOptions: parameterMap.runOptions,
                pathRoot: parameterMap.pathRoot,
                appName: appName,
                archiveName: archiveName,
                runArgs: parameterMap.runArgs
        ]
        steps.writeFile file: "${shellPath}", text: TemplateUtils.makeTemplate(serviceTemplate,templateData)
        steps.sh "chmod +x ${shellPath}"
        steps.dir("${pathRoot}/${appName}"){
            steps.withEnv(["JENKINS_NODE_COOKIE=dontKillMe"]) {
                steps.sh "$shellPath restart"
            }
        }

    }

}
