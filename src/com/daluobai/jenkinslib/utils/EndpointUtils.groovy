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
    def healthCheckWithLocalTCPPort(def localTCPPort,def period,def failureThreshold) {
        Assert.notNull(localTCPPort,"端口为空")
        if (ObjectUtil.isNull(period)){
            period = 0
        }
        if (ObjectUtil.isNull(failureThreshold) || failureThreshold < 1){
            period = 1
        }
        steps.echo "检查本地端口是否监听:${localTCPPort}"
        boolean isOnline = false
        for (int i = 0; i < failureThreshold; i++) {
            steps.echo "健康检查-第${i}次"
            sleep period
            def portListening = steps.sh returnStdout: true, script: """netstat -an | egrep ":${localTCPPort}" | awk '\$1 ~ /tcp/ && \$NF == "LISTEN" {print \$0}' | wc -l"""
            int portListeningNum = portListening.trim()
            if (portListeningNum > 0){
                steps.echo "端口监听成功:${portListeningNum},${localTCPPort}"
                isOnline = true
                break
            }
        }
        return isOnline;
    }
}