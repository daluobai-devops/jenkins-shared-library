package com.daluobai.jenkinslib.api

import com.daluobai.jenkinslib.utils.AssertUtils
import com.daluobai.jenkinslib.utils.JsonUtils
/**
 * @author daluobai@outlook.com
 * version 1.0.0
 * @title 
 * @description https://github.com/daluobai-devops/jenkins-shared-library
 * @create 2023/4/25 12:10
 */
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
        AssertUtils.notBlank(deployName,"deployName空的");
        AssertUtils.notBlank(namespace,"namespace空的");

        def deployStatusMap = steps.sh returnStdout: true, script: "kubectl get deploy ${deployName} -n ${namespace} -o jsonpath='{.status}'"
        JsonUtils.JSONObject responseJson = null;
        try{
            responseJson = JsonUtils.parseObj(deployStatusMap);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return responseJson;

    }
}