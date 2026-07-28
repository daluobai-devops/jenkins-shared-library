package com.daluobai.jenkinslib.steps

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
        return deploy(parameterMap, steps.globalParameterMap as Map)
    }

    // 当前交付 implementation 显式传入有效配置；单参数入口仅供旧调用兼容。
    def deploy(Map parameterMap, Map effectiveConfig) {
        steps.echo "开始执行Java Web部署编排"
        AssertUtils.notEmpty(parameterMap, "参数为空")
        def labels = parameterMap.labels
        def readinessProbeMap = parameterMap.readinessProbe
        def afterRunCMD = parameterMap.afterRunCMD
        def globalParameterMap = effectiveConfig
        AssertUtils.notEmpty(labels, "labels为空")
        if (globalParameterMap.DEPLOY_PIPELINE?.stepsStorage?.jenkinsStash?.enable != true) {
            steps.error '文件部署要求DEPLOY_PIPELINE.stepsStorage.jenkinsStash.enable=true'
        }
        Map javaDeployConfig = parameterMap.stepsJavaWebDeployToService as Map
        Map tomcatDeployConfig = parameterMap.stepsTomcatDeploy as Map
        boolean javaDeployEnabled = ObjUtils.isNotEmpty(javaDeployConfig) && javaDeployConfig.enable != false
        boolean tomcatDeployEnabled = ObjUtils.isNotEmpty(tomcatDeployConfig) && tomcatDeployConfig.enable != false
        if (javaDeployEnabled && tomcatDeployEnabled) {
            steps.error 'stepsJavaWebDeployToService与stepsTomcatDeploy不能同时启用'
        }
        if (!javaDeployEnabled && !tomcatDeployEnabled) {
            steps.error '没有启用任何Java Web部署方式'
        }

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
                    if (javaDeployEnabled) {
                        stepsJavaWeb.deploy(javaDeployConfig, globalParameterMap)
                    } else {
                        stepsTomcat.deploy(tomcatDeployConfig, globalParameterMap)
                    }
                    //健康检查
                    if (parameterMap.readinessSequence instanceof Collection) {
                        verifyReadinessInOrder(parameterMap.readinessSequence as Collection)
                    } else if (readinessProbeMap != null) {
                        def healthAll = true
                        if (healthAll && ObjUtils.isNotEmpty(readinessProbeMap.tcp) && (readinessProbeMap.tcp.enable == null || readinessProbeMap.tcp.enable)) {
                            def healthCheck = endpointUtils.healthCheckWithLocalTCPPort(readinessProbeMap.tcp.port, readinessProbeMap.period, readinessProbeMap.failureThreshold)
                            if (!healthCheck) {
                                healthAll = false
                                steps.echo "healthCheckWithLocalTCPPort，检查失败"
                            }
                            steps.echo "healthCheckWithLocalTCPPort结束，${healthCheck}"
                        }
                        if (healthAll && ObjUtils.isNotEmpty(readinessProbeMap.http) && (readinessProbeMap.http.enable == null || readinessProbeMap.http.enable)) {
                            def healthCheck = endpointUtils.healthCheckWithHttp("http://localhost:${readinessProbeMap.http.port}${readinessProbeMap.http.path}", readinessProbeMap.http.timeout, readinessProbeMap.period, readinessProbeMap.failureThreshold)
                            if (!healthCheck) {
                                healthAll = false
                                steps.echo "healthCheckWithHttp，检查失败"
                            }
                            steps.echo "healthCheckWithHttp结束，${healthCheck}"
                        }
                        if (healthAll && ObjUtils.isNotEmpty(readinessProbeMap.cmd) && (readinessProbeMap.cmd.enable == null || readinessProbeMap.cmd.enable)) {
                            def healthCheck = endpointUtils.healthCheckWithCMD(readinessProbeMap.cmd.command, readinessProbeMap.cmd.timeout, readinessProbeMap.period, readinessProbeMap.failureThreshold)
                            if (!healthCheck) {
                                healthAll = false
                                steps.echo "healthCheckWithCMD，检查失败"
                            }
                            steps.echo "healthCheckWithCMD结束，${healthCheck}"
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

    private void verifyReadinessInOrder(Collection checks) {
        checks.each { Object rawCheck ->
            Map check = rawCheck as Map
            Map config = (check.config ?: [:]) as Map
            boolean ready
            if (check.type == 'TCP') {
                ready = endpointUtils.healthCheckWithLocalTCPPort(config.port, config.period, config.failureThreshold)
            } else if (check.type == 'HTTP') {
                ready = endpointUtils.healthCheckWithHttp(
                        "http://localhost:${config.port}${config.path}", config.timeout, config.period, config.failureThreshold
                )
            } else if (check.type == 'COMMAND') {
                ready = endpointUtils.healthCheckWithCMD(config.command, config.timeout, config.period, config.failureThreshold)
            } else {
                steps.error "不支持的就绪验证: ${check.type}"
                return
            }
            steps.echo "就绪验证${check.type}结束，${ready}"
            if (!ready) {
                steps.error '服务未就绪'
            }
        }
    }
}
