package com.daluobai.jenkinslib.steps

@Grab('org.reflections:reflections:0.9.9-RC1')
@Grab('cn.hutool:hutool-all:5.8.11')
import cn.hutool.core.date.DateUtil
import cn.hutool.core.lang.Assert
import cn.hutool.core.util.ObjectUtil
import cn.hutool.core.util.StrUtil
import com.daluobai.jenkinslib.constant.GlobalShare
import com.daluobai.jenkinslib.utils.EndpointUtils

/**
 * @author daluobai@outlook.com
 * version 1.0.0
 * @title 发布到tomcat
 * @description https://github.com/daluobai-devops/jenkins-shared-library
 * @create 2023/4/25 12:10
 */
class StepsDeploy implements Serializable {
    def steps

    StepsDeploy(steps) { this.steps = steps }

    /*******************初始化全局对象 开始*****************/
    def stepsJenkins = new StepsJenkins(steps)
    def endpointUtils = new EndpointUtils(steps)
    def stepsTomcat = new StepsTomcat(steps)
    def stepsJavaWeb = new StepsJavaWeb(steps)
    /*******************初始化全局对象 结束*****************/

    //发布
    def deploy(Map parameterMap) {
        steps.echo "StepsJavaWeb:${parameterMap}"
        Assert.notEmpty(parameterMap, "参数为空")
        def labels = parameterMap.labels
        def enable = parameterMap.enable
        def readinessProbeMap = parameterMap.readinessProbe
        def afterRunCMD = parameterMap.afterRunCMD
        def appName = GlobalShare.globalParameterMap.SHARE_PARAM.appName
        def archiveName = GlobalShare.globalParameterMap.SHARE_PARAM.archiveName
        //获取文件名后缀
        def archiveSuffix = StrUtil.subAfter(archiveName, ".", true)
        //获取文件名
        def archiveOnlyName = StrUtil.subBefore(archiveName, ".", true)
        Assert.notEmpty(labels, "labels为空")

        def backAppName = "app-" + DateUtil.format(new Date(), "yyyyMMddHHmmss") + "." + archiveSuffix
        steps.withCredentials([steps.sshUserPrivateKey(credentialsId: 'ssh-jenkins', keyFileVariable: 'SSH_KEY_PATH')]) {
            steps.sh "cat /etc/hostname && pwd && mkdir -p ~/.ssh && chmod 700 ~/.ssh && rm -f ~/.ssh/id_rsa && cp \${SSH_KEY_PATH} ~/.ssh/id_rsa || true && chmod 600 ~/.ssh/id_rsa"
        }
        labels.each { c ->
            def label = c
            steps.echo "发布第一个标签:${label}"
            def nodeDeployNodeList = stepsJenkins.getNodeByLabel(label)
            steps.echo "获取到发布节点:${nodeDeployNodeList}"
            if (ObjectUtil.isEmpty(nodeDeployNodeList)) {
                steps.error '没有可用的发布节点'
            }
            nodeDeployNodeList.each { d ->
                def nodeDeployNode = d
                steps.echo "开始发布:${nodeDeployNode}"
                steps.node(nodeDeployNode) {
                    if (ObjectUtil.isNotEmpty(parameterMap.stepsJavaWebDeployToService)) {
                        stepsJavaWeb.deploy(parameterMap.stepsJavaWebDeployToService)
                    } else if (ObjectUtil.isNotEmpty(parameterMap.stepsTomcatDeploy)) {
                        stepsTomcat.deploy(parameterMap.stepsJavaWebDeployToTomcat)
                    }
                    //健康检查
                    if (readinessProbeMap != null) {
                        def healthAll = true
                        if (ObjectUtil.isNotEmpty(readinessProbeMap.tcp) && (readinessProbeMap.tcp.enable == null || readinessProbeMap.tcp.enable)) {
                            def healthCheck = endpointUtils.healthCheckWithLocalTCPPort(readinessProbeMap.tcp.port, readinessProbeMap.period, readinessProbeMap.failureThreshold)
                            if (!healthCheck) {
                                healthAll = false
                                steps.echo "healthCheckWithLocalTCPPort，检查失败"
                            }
                        }
                        if (ObjectUtil.isNotEmpty(readinessProbeMap.http) && (readinessProbeMap.http.enable == null || readinessProbeMap.http.enable)) {
                            def healthCheck = endpointUtils.healthCheckWithHttp("http://localhost:${readinessProbeMap.http.port}${readinessProbeMap.http.path}", readinessProbeMap.http.timeout, readinessProbeMap.period, readinessProbeMap.failureThreshold)
                            if (!healthCheck) {
                                healthAll = false
                                steps.echo "healthCheckWithHttp，检查失败"
                            }
                        }
                        if (ObjectUtil.isNotEmpty(readinessProbeMap.cmd) && (readinessProbeMap.cmd.enable == null || readinessProbeMap.cmd.enable)) {
                            def healthCheck = endpointUtils.healthCheckWithCMD(readinessProbeMap.cmd.command, readinessProbeMap.cmd.timeout, readinessProbeMap.period, readinessProbeMap.failureThreshold)
                            if (!healthCheck) {
                                healthAll = false
                                steps.echo "healthCheckWithCMD，检查失败"
                            }
                        }
                        if (!healthAll) {
                            steps.error '服务未就绪'
                        }
                    }
                    //所有部署流程执行完成后运行的命令
                    if (StrUtil.isNotBlank(afterRunCMD)) {
                        steps.sh "${afterRunCMD}"
                    }
                }
            }
        }
    }
}