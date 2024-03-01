package com.daluobai.jenkinslib.utils

import cn.hutool.core.util.ObjectUtil
@Grab('cn.hutool:hutool-all:5.8.11')
import cn.hutool.http.HttpUtil
import cn.hutool.core.lang.Assert
import cn.hutool.json.JSONUtil
import cn.hutool.core.util.StrUtil
/**
 * @author daluobai@outlook.com
 * version 1.0.0
 * @title 
 * @description https://github.com/daluobai-devops/jenkins-shared-library
 * @create 2023/4/25 12:10
 */
class EndpointUtils implements Serializable {
    def steps

    EndpointUtils(steps) { this.steps = steps }

    /**
     * 健康检查 重试60次，每次等待3s.
     * @param heathcheckUrl 健康检查的链接
     * @return
     */
    def healthCheck(String heathcheckUrl) {
        Assert.notBlank(heathcheckUrl,"heathcheckUrl为空");
        steps.echo "健康检查-路径:${heathcheckUrl}"
        boolean isOnline = false
        for (int i = 0; i < 60; i++) {
            steps.echo "健康检查-第${i}次"
            sleep 3000
            String response = "";
            try {
                response = HttpUtil.get(heathcheckUrl)
            } catch (Exception e) {

            }
            if (StrUtil.isBlank(response) || !JSONUtil.isJson(response)){
                continue
            }
            cn.hutool.json.JSONObject responseJson = JSONUtil.parseObj(response);

            String status = responseJson.getStr("status");
            if (StrUtil.isNotBlank(status) && status.equals("UP")){
                isOnline = true
                break
            }
        }

        return isOnline;
    }

    /**
     * 健康检查 重试60次，每次等待3s.
     * @param deployName
     * @param namespace
     * @return
     */
    def kubernetesDeployStatusCheck(String deployName,String namespace) {
        Assert.notBlank(deployName,"deployName空的");
        Assert.notBlank(namespace,"namespace空的");
        def kubernetesApi = new com.daluobai.jenkinslib.api.KubernetesApi(steps)
        steps.echo "发布状态检查:${deployName} ${namespace}"
        boolean isOnline = false
        for (int i = 0; i < 60; i++) {
            steps.echo "发布状态检查-第${i}次"
            sleep 3000
            def deployStatusMap = kubernetesApi.deploymentStatus(deployName,namespace)
            steps.echo "发布状态检查:${deployStatusMap}"
            if (deployStatusMap == null){
                continue
            }
            int replicas = deployStatusMap.getStr("replicas");
            int readyReplicas = deployStatusMap.getStr("readyReplicas");
            if (replicas > 0 && replicas == readyReplicas){
                isOnline = true
                break
            }
        }

        return isOnline;
    }

    /**
     * 检查本地端口是否监听
     * @param localTCPPort
     * @return
     */
    def healthCheckWithLocalTCPPort(def localTCPPort,def periodSec,def failureThreshold) {
        Assert.notNull(localTCPPort,"端口为空")
        steps.echo "检查本地端口是否监听-参数${localTCPPort}，间隔${periodSec}，重试次数${failureThreshold}"
        if (ObjectUtil.isNull(periodSec) || periodSec < 0){
            periodSec = 0
        }
        if (ObjectUtil.isNull(failureThreshold) || failureThreshold < 1){
            failureThreshold = 1
        }
        def periodMS = periodSec * 1000
        steps.echo "检查本地端口是否监听:${localTCPPort}"
        boolean isOnline = false
        for (int i = 0; i < failureThreshold; i++) {
            steps.echo "健康检查-第${i}次"
            sleep periodMS
            //加上wc -l会导致结果不对，所以按照是否有返回值判断
            def portListeningStr = steps.sh returnStdout: true, script: """ss -tuln | egrep '^.*${localTCPPort}\\s' | awk '\$1 ~ /tcp/ && \$2 == "LISTEN" {print \$0}'"""
            boolean portListening = ObjectUtil.isNotEmpty(portListeningStr) && ObjectUtil.isNotEmpty(portListeningStr.trim())
            if (portListening){
                steps.echo "端口监听成功:${portListeningStr},${localTCPPort}"
                isOnline = true
                break
            }
        }
        return isOnline;
    }

    /**
     * 检查http接口是否正常
     * @param localTCPPort
     * @return
     */
    def healthCheckWithHttp(def url,def timeout,def periodSec,def failureThreshold) {
        Assert.notNull(url,"url为空")
        steps.echo "检查http是能访问-参数${url}，${timeout}，间隔${periodSec}，重试次数${failureThreshold}"
        if (ObjectUtil.isNull(periodSec) || periodSec < 0){
            periodSec = 0
        }
        if (ObjectUtil.isNull(failureThreshold) || failureThreshold < 1){
            failureThreshold = 1
        }
        if (ObjectUtil.isNull(timeout) || timeout < 1){
            timeout = 5
        }
        def periodMS = periodSec * 1000
        boolean isOnline = false
        for (int i = 0; i < failureThreshold; i++) {
            steps.echo "健康检查-第${i}次"
            sleep periodMS
            def httpCode = "0"
            try {
                httpCode = steps.sh returnStdout: true, script: """curl -s -o /dev/null -w '%{http_code}' --connect-timeout ${timeout} ${url}"""
            } catch (Exception e) {
            }
            boolean httpListening = ObjectUtil.isNotEmpty(httpCode) && httpCode.trim() == "200"
            if (httpListening){
                steps.echo "url访问成功:${url},${timeout}"
                isOnline = true
                break
            }
        }
        return isOnline;
    }

    /**
     * 检查http接口是否正常
     * @param localTCPPort
     * @return
     */
    def healthCheckWithCMD(def command,def timeout,def periodSec,def failureThreshold) {
        Assert.notNull(command,"command为空")
        steps.echo "检查http是能访问-参数${command}，${timeout}，间隔${periodSec}，重试次数${failureThreshold}"
        if (ObjectUtil.isNull(periodSec) || periodSec < 0){
            periodSec = 0
        }
        if (ObjectUtil.isNull(failureThreshold) || failureThreshold < 1){
            failureThreshold = 1
        }
        if (ObjectUtil.isNull(timeout) || timeout < 1){
            timeout = 5
        }
        def periodMS = periodSec * 1000
        boolean isOnline = false
        for (int i = 0; i < failureThreshold; i++) {
            steps.echo "健康检查-第${i}次"
            sleep periodMS
            def exitCode = 1
            try {
                steps.timeout(time: timeout, unit: 'SECONDS') {
                    exitCode = steps.sh label: '执行command参数', returnStatus: true, script: command
                }
            } catch (Exception e) {
                exitCode = 1
            }
            boolean isSuccess = ObjectUtil.isNotEmpty(exitCode) && exitCode.trim() == "0"
            if (isSuccess){
                steps.echo "CMD执行成功:${command},${timeout}"
                isOnline = true
                break
            }
        }
        return isOnline;
    }
}