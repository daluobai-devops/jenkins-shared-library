package com.daluobai.jenkinslib.utils

@Grab('cn.hutool:hutool-all:5.8.11')
import cn.hutool.http.HttpUtil
import cn.hutool.core.lang.Assert
import cn.hutool.json.JSONObject
import cn.hutool.json.JSONUtil
import cn.hutool.core.util.StrUtil
import com.daluobai.jenkinslib.api.KubernetesApi

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
}