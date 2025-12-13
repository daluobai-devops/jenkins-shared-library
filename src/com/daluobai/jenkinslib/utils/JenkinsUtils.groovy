package com.daluobai.jenkinslib.utils

import com.daluobai.jenkinslib.utils.AssertUtils
import com.daluobai.jenkinslib.constant.EFileReadType

/**
 * @author daluobai@outlook.com
 * version 1.0.0
 * @title 
 * @description https://github.com/daluobai-devops/jenkins-shared-library
 * @create 2023/4/25 12:10
 */
class JenkinsUtils implements Serializable {

    def steps

    JenkinsUtils(steps) { this.steps = steps }

    /**
     * pipeline sh
     * @param script
     * @return
     */
    def pipelineSH(String script) {
        AssertUtils.notBlank(script, "script为空");
        def scriptResp = steps.sh returnStdout: true, script: script
        def scriptRespTrim = scriptResp.trim()
        return scriptRespTrim
    }
}