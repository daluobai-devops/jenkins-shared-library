package com.daluobai.jenkinslib.steps

@Grab('org.reflections:reflections:0.9.9-RC1')
import com.daluobai.jenkinslib.utils.DateUtils
import com.daluobai.jenkinslib.utils.AssertUtils
import com.daluobai.jenkinslib.utils.ObjUtils
import com.daluobai.jenkinslib.utils.StrUtils
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
        AssertUtils.notEmpty(parameterMap, "参数为空")
        def labels = parameterMap.labels
        def enable = parameterMap.enable
        def readinessProbeMap = parameterMap.readinessProbe
        def afterRunCMD = parameterMap.afterRunCMD
        def globalParameterMap = steps.globalParameterMap
        def appName = globalParameterMap.SHARE_PARAM.appName
        def archiveName = globalParameterMap.SHARE_PARAM.archiveName
        //获取文件名后缀
        def archiveSuffix = StrUtils.subAfter(archiveName, ".", true)
        //获取文件名
        def archiveOnlyName = StrUtils.subBefore(archiveName, ".", true)
        AssertUtils.notEmpty(labels, "labels为空")

        def backAppName = "app-" + DateUtils.format(new Date(), "yyyyMMddHHmmss") + "." + archiveSuffix

        labels.each { c ->
            def label = c
            steps.echo "发布节点:${label}"
            def nodeDeployNodeList = stepsJenkins.getNodeByLabel(label)
            steps.echo "获取到发布节点:${nodeDeployNodeList}"
            if (ObjUtils.isEmpty(nodeDeployNodeList)) {
                steps.error '没有可用的发布节点'
            }
            nodeDeployNodeList.each { d ->
                def nodeDeployNode = d
                steps.echo "开始发布:${nodeDeployNode}"
                steps.node(nodeDeployNode) {
                    if (ObjUtils.isNotEmpty(parameterMap.stepsJavaWebDeployToService)) {
                        stepsJavaWeb.deploy(parameterMap.stepsJavaWebDeployToService)
                    } else if (ObjUtils.isNotEmpty(parameterMap.stepsTomcatDeploy)) {
                        stepsTomcat.deploy(parameterMap.stepsJavaWebDeployToTomcat)
                    }
                    //健康检查
                    if (readinessProbeMap != null) {
                        def healthAll = true
                        if (healthAll && ObjUtils.isNotEmpty(readinessProbeMap.tcp) && (readinessProbeMap.tcp.enable == null || readinessProbeMap.tcp.enable)) {
                            def healthCheck = endpointUtils.healthCheckWithLocalTCPPort(readinessProbeMap.tcp.port, readinessProbeMap.period, readinessProbeMap.failureThreshold)
                            if (!healthCheck) {
                                healthAll = false
                                steps.echo "healthCheckWithLocalTCPPort，检查失败"
                            }
                            steps.echo "tcp，${healthCheck}"
                        }
                        if (healthAll && ObjUtils.isNotEmpty(readinessProbeMap.http) && (readinessProbeMap.http.enable == null || readinessProbeMap.http.enable)) {
                            def healthCheck = endpointUtils.healthCheckWithHttp("http://localhost:${readinessProbeMap.http.port}${readinessProbeMap.http.path}", readinessProbeMap.http.timeout, readinessProbeMap.period, readinessProbeMap.failureThreshold)
                            if (!healthCheck) {
                                healthAll = false
                                steps.echo "healthCheckWithHttp，检查失败"
                            }
                            steps.echo "http，${healthCheck}"
                        }
                        if (healthAll && ObjUtils.isNotEmpty(readinessProbeMap.cmd) && (readinessProbeMap.cmd.enable == null || readinessProbeMap.cmd.enable)) {
                            def healthCheck = endpointUtils.healthCheckWithCMD(readinessProbeMap.cmd.command, readinessProbeMap.cmd.timeout, readinessProbeMap.period, readinessProbeMap.failureThreshold)
                            if (!healthCheck) {
                                healthAll = false
                                steps.echo "healthCheckWithCMD，检查失败"
                            }
                            steps.echo "cmd，${healthCheck}"
                        }
                        if (!healthAll) {
                            steps.error '服务未就绪'
                        }
                    }
                    //所有部署流程执行完成后运行的命令
                    if (StrUtils.isNotBlank(afterRunCMD)) {
                        steps.sh "${afterRunCMD}"
                    }
                }
            }
        }
    }
}