package com.daluobai.jenkinslib.steps

@Grab('org.reflections:reflections:0.9.9-RC1')
@Grab('cn.hutool:hutool-all:5.8.11')
import cn.hutool.core.date.DateUtil
import cn.hutool.core.lang.Assert

import cn.hutool.core.util.ObjectUtil
import cn.hutool.core.util.StrUtil
import com.daluobai.jenkinslib.constant.GlobalShare
import com.daluobai.jenkinslib.utils.EndpointUtils
import com.daluobai.jenkinslib.utils.TemplateUtils

/**
 * @author daluobai@outlook.com
 * version 1.0.0
 * @title 发布到tomcat
 * @description https://github.com/daluobai-devops/jenkins-shared-library
 * @create 2023/4/25 12:10
 */
class StepsTomcat implements Serializable {
    def steps

    StepsTomcat(steps) { this.steps = steps }

    /*******************初始化全局对象 开始*****************/
    def stepsJenkins = new StepsJenkins(steps)
    def endpointUtils = new EndpointUtils(steps)
    /*******************初始化全局对象 结束*****************/

    //发布
    def deploy(Map parameterMap) {
        steps.echo "StepsJavaWeb:${parameterMap}"
        Assert.notEmpty(parameterMap,"参数为空")
        def enable = parameterMap.enable
        if (enable == false) {
            steps.echo "StepsTomcat.deploy不执行"
            return
        }
        def tomcatHome = parameterMap.tomcatHome
        def deployPath = parameterMap.deployPath
        def command = parameterMap.command
        def globalParameterMap = steps.binding.getVariable("globalParameterMap")
        def appName = globalParameterMap.SHARE_PARAM.appName
        def archiveName = globalParameterMap.SHARE_PARAM.archiveName
        //获取文件名后缀
        def archiveSuffix = StrUtil.subAfter(archiveName, ".", true)
        //获取文件名
        def archiveOnlyName = StrUtil.subBefore(archiveName, ".", true)

        def backAppName = "app-" + DateUtil.format(new Date(), "yyyyMMddHHmmss") + "." + archiveSuffix
//        steps.withCredentials([steps.sshUserPrivateKey(credentialsId: 'ssh-jenkins', keyFileVariable: 'SSH_KEY_PATH')]) {
//            steps.sh "mkdir -p ~/.ssh && chmod 700 ~/.ssh && rm -f ~/.ssh/id_rsa && cp \${SSH_KEY_PATH} ~/.ssh/id_rsa && chmod 600 ~/.ssh/id_rsa"
//        }

        steps.unstash("appPackage")
        steps.sh "hostname"
        steps.sh "ls -l package"
        steps.sh "mkdir -p ${deployPath}/ && mkdir -p ${tomcatHome} && mkdir -p ${tomcatHome}/backup/${appName}"
        //备份
        steps.sh "mv ${deployPath}/${archiveName} ${tomcatHome}/backup/${appName}/${backAppName}/ || true"
        steps.dir("${tomcatHome}/backup/${appName}/"){
            steps.sh "find . -mtime +3 -delete"
        }
        //拷贝新的包到发布目录
        steps.sh "cp package/${archiveName} ${deployPath}"
        //创建一个 app 文件夹把包解压到里面
        steps.sh "rm -rf ${deployPath}/${archiveOnlyName}/ || true"
        //切换到发布目录
        steps.dir("${deployPath}/"){
            if (ObjectUtil.isNotEmpty(command)){
                steps.sh "${command}"
            }
        }
    }

}
