package com.daluobai.jenkinslib.steps

import cn.hutool.core.lang.Assert
@Grab('cn.hutool:hutool-all:5.8.11')

import cn.hutool.core.util.StrUtil
import com.daluobai.jenkinslib.utils.ConfigUtils
import com.daluobai.jenkinslib.utils.FileUtils

class StepsBuildNpm implements Serializable {
    def steps

    StepsBuildNpm(steps) { this.steps = steps }

    /*******************初始化全局对象 开始*****************/
    def stepsGit = new StepsGit(steps)
    def configUtils = new ConfigUtils(steps)
    def fileUtils = new FileUtils(steps)
    /*******************初始化全局对象 结束*****************/

    //构建
    def build(Map configMap) {
        //默认配置
        def configDefault = configMap["DEFAULT_CONFIG"]//默认配置
        //共享配置
        def configShare = configMap["SHARE_PARAM"]
        //流程配置
        def configSteps = configMap.DEPLOY_PIPELINE.stepsBuildNpm

        Assert.notNull(configDefault, "DEFAULT_CONFIG为空")
        Assert.notNull(configShare, "SHARE_PARAM为空")
        Assert.notNull(configSteps, "DEPLOY_PIPELINE.stepsBuildMaven为空")

        def pathBase = "/app"
        //docker-构建产物目录
        def pathPackage = "package"
        //docker-代码目录
        def pathCode = "code"
        //宿主机目录-构建产物目录
        def hostPathPackage = "${steps.env.WORKSPACE}/${pathPackage}"

        steps.sh "mkdir -p ${hostPathPackage}"

        def dockerBuildImage = StrUtil.isNotBlank(configSteps.dockerBuildImage) ? configSteps.dockerBuildImage : "registry.cn-hangzhou.aliyuncs.com/wuzhaozhongguo/build-npm:10.16.0"
        def dockerBuildImageUrl = "${dockerBuildImage}"

        //如果没有提供登录密钥则不登录
        def dockerLoginDomain = StrUtil.isNotBlank(configDefault.docker.registry.credentialsId) ? "https://${configDefault.docker.registry.domain}" : ""
        def dockerLoginCredentialsId = StrUtil.isNotBlank(configDefault.docker.registry.credentialsId) ? configDefault.docker.registry.credentialsId : ""

        steps.withDockerRegistry(credentialsId: dockerLoginCredentialsId, url: dockerLoginDomain) {

            def mavenImage = steps.docker.image("${dockerBuildImageUrl}")
            mavenImage.pull()

            def mvnCMDSubMod = "-pl ${configSteps.subModule} -am -amd"
            def mvnCMDActiveProfile = "-P ${configSteps.activeProfile}"

            mavenImage.inside("--entrypoint '' -v maven-repo:/root/.m2/repository -v ${steps.env.WORKSPACE}/${pathPackage}:/app/package") {
                //从 jenkins 凭据管理中获取密钥文件路径并且拷贝到~/.ssh/id_rsa
                steps.withCredentials([steps.sshUserPrivateKey(credentialsId: 'ssh-git', keyFileVariable: 'SSH_KEY_PATH')]) {
                    steps.sh "mkdir -p ~/.ssh && chmod 700 ~/.ssh && rm -f ~/.ssh/id_rsa && cp \${SSH_KEY_PATH} ~/.ssh/id_rsa && chmod 600 ~/.ssh/id_rsa"
                }
                //生成known_hosts
                stepsGit.sshKeyscan("${configSteps.gitUrl}", "~/.ssh/known_hosts")
                steps.sh """
                        #! /bin/bash -eu
                        set -eo pipefail
                        mkdir -p ${pathBase}/${pathPackage} && mkdir -p ${pathBase}/${pathCode}
                        cd ${pathBase}/${pathCode}
                        git config --global http.version HTTP/1.1
                        git clone ${configSteps.gitUrl} --branch ${configSteps.gitBranch} --single-branch --depth 1 --quiet
                        mv ${pathBase}/${pathCode}/\$(ls -A1 ${pathBase}/${pathCode}/) ${pathBase}/${pathCode}/${pathCode}
                        cd ${pathBase}/${pathCode}/${pathCode}
                        git config core.ignorecase false
                        ${configSteps.buildCMD}
                        ls -al ${pathBase}/${pathCode}/${pathCode}/dist
                        cp -r ${pathBase}/${pathCode}/${pathCode}/dist ${pathBase}/${pathPackage}/
                    """
            }
        }
    }
}
