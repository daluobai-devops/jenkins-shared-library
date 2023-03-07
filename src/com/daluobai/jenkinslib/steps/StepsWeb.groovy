package com.daluobai.jenkinslib.steps

import cn.hutool.core.lang.Assert

//@Grab('cn.hutool:hutool-all:5.8.11')

import cn.hutool.core.util.ObjectUtil
import cn.hutool.core.util.StrUtil
import com.daluobai.jenkinslib.constant.GlobalShare
import com.daluobai.jenkinslib.utils.TemplateUtils

class StepsWeb implements Serializable {
    def steps

    StepsWeb(steps) { this.steps = steps }

    /*******************初始化全局对象 开始*****************/
    def stepsJenkins = new StepsJenkins(steps)
    /*******************初始化全局对象 结束*****************/

    //发布
    def deploy(Map parameterMap) {
        steps.echo "StepsJavaWeb:${parameterMap}"
        Assert.notEmpty(parameterMap,"参数为空")
        def labels = parameterMap.labels
        def pathRoot = parameterMap.pathRoot
        def appName = GlobalShare.globalParameterMap.appName
        def archiveName = GlobalShare.globalParameterMap.archiveName
        //获取文件名后缀
        def archiveSuffix = StrUtil.subAfter(archiveName, ".", true)
        Assert.notEmpty(labels,"labels为空")

        def backAppName = "app-" + System.currentTimeMillis() + "." + archiveSuffix
        steps.withCredentials([steps.sshUserPrivateKey(credentialsId: 'ssh-jenkins', keyFileVariable: 'SSH_KEY_PATH')]) {
            steps.sh "mkdir -p ~/.ssh && chmod 700 ~/.ssh && rm -f ~/.ssh/id_rsa && cp \${SSH_KEY_PATH} ~/.ssh/id_rsa && chmod 600 ~/.ssh/id_rsa"
        }
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
                    //创建一个 app 文件夹把包解压到里面
                    steps.sh "rm -rf ${pathRoot}/${appName}/app || true"
                    steps.sh "mkdir -p ${pathRoot}/${appName}/app"
                    //切换到发布目录
                    steps.dir("${pathRoot}/${appName}/"){
                        steps.sh "tar -zxvf ${archiveName} -C app/"
                        steps.sh "ls -l app"
                    }
                }
            }
        }
    }

}
