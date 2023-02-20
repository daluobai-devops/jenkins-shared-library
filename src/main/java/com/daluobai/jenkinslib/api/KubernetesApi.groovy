package com.daluobai.jenkinslib.api

@Grab('cn.hutool:hutool-all:5.8.11')
import cn.hutool.http.HttpUtil
import cn.hutool.core.lang.Assert
import cn.hutool.json.JSONObject
import cn.hutool.json.JSONUtil
import cn.hutool.core.util.StrUtil

class KubernetesApi implements Serializable {
    def steps

    KubernetesApi(steps) { this.steps = steps }

    /**
     * 获取deployment状态
     * @param deployment
     * @param namespace
     * @return {"availableReplicas":1,"observedGeneration":3,"readyReplicas":1,"replicas":1,"updatedReplicas":1}
     */
    def deploymentStatus(String deployName,String namespace) {
        Assert.notBlank(deployName,"deployName空的");
        Assert.notBlank(namespace,"namespace空的");

        def deployStatusMap = steps.sh returnStdout: true, script: "kubectl get deploy ${deployName} -n ${namespace} -o jsonpath='{.status}'"
        cn.hutool.json.JSONObject responseJson = null;
        try{
            responseJson = JSONUtil.parseObj(deployStatusMap);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return responseJson;

    }
}