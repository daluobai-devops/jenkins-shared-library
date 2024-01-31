package com.daluobai.jenkinslib.steps

@Grab('cn.hutool:hutool-all:5.8.11')

import cn.hutool.core.util.StrUtil
import cn.hutool.core.lang.Assert
import com.daluobai.jenkinslib.utils.ConfigUtils
import com.daluobai.jenkinslib.utils.FileUtils
/**
 * @author daluobai@outlook.com
 * version 1.0.0
 * @title 
 * @description https://github.com/daluobai-devops/jenkins-shared-library
 * @create 2023/4/25 12:10
 */
class StepsBuildMaven implements Serializable {
    def steps

    StepsBuildMaven(steps) { this.steps = steps }

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
        def configSteps = configMap.DEPLOY_PIPELINE.stepsBuildMaven

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

        def dockerBuildImage = StrUtil.isNotBlank(configSteps.dockerBuildImage) ? configSteps.dockerBuildImage : "registry.cn-hangzhou.aliyuncs.com/wuzhaozhongguo/build-maven:3.8.5-jdk8"
        def dockerBuildImageUrl = "${dockerBuildImage}"

        //获取settings.xml配置，如果没有设置则为空
        def settingsXmlStr = StrUtil.isNotBlank(configSteps.settingsFullPath) ? fileUtils.readStringFromFullPath(configSteps.settingsFullPath) : null

        //如果没有提供登录密钥则不登录
        def dockerLoginDomain = StrUtil.isNotBlank(configDefault.docker.registry.credentialsId) ? "https://${configDefault.docker.registry.domain}" : ""
        def dockerLoginCredentialsId = StrUtil.isNotBlank(configDefault.docker.registry.credentialsId) ? configDefault.docker.registry.credentialsId : ""

        steps.withDockerRegistry(credentialsId: dockerLoginCredentialsId, url: dockerLoginDomain) {

            def mavenImage = steps.docker.image("${dockerBuildImageUrl}")
            mavenImage.pull()

            def mvnCMDSubMod = "-pl ${configSteps.subModule} -am -amd"
            def mvnCMDActiveProfile = StrUtil.isNotEmpty(configSteps.activeProfile) ? "-P ${configSteps.activeProfile}" : ""

            mavenImage.inside("--entrypoint '' -v maven-repo:/root/.m2/repository -v ${steps.env.WORKSPACE}/${pathPackage}:/app/package") {
                //从 jenkins 凭据管理中获取密钥文件路径并且拷贝到~/.ssh/id_rsa
                stepsGit.saveJenkinsSSHKey('ssh-git')
                //生成known_hosts
                stepsGit.sshKeyscan("${configSteps.gitUrl}", "~/.ssh/known_hosts")
                //如果有settings.xml配置则写入用户自定义配置.
                if (StrUtil.isNotBlank(settingsXmlStr)){
                    fileUtils.writeFileBySH("~/.m2/settings.xml", settingsXmlStr)
                }
                steps.sh """
                        #! /bin/sh -e
                        set -eo pipefail
                        mkdir -p ${pathBase}/${pathPackage} && mkdir -p ${pathBase}/${pathCode}
                        cd ${pathBase}/${pathCode}
                        git config --global http.version HTTP/1.1
                        git clone ${configSteps.gitUrl} --branch ${configSteps.gitBranch} --single-branch --depth 1 --quiet
                        mv ${pathBase}/${pathCode}/\$(ls -A1 ${pathBase}/${pathCode}/) ${pathBase}/${pathCode}/${pathCode}
                        cd ${pathBase}/${pathCode}/${pathCode}
                        git log --pretty=format:"%h -%an,%ar : %s" -3
                        git config core.ignorecase false
                        mvn -Dmaven.test.skip=${configSteps.skipTest} ${configSteps.lifecycle} -Dmaven.compile.fork=true -U -B ${mvnCMDSubMod} ${mvnCMDActiveProfile}
                        ls -al ${pathBase}/${pathCode}/${pathCode}/${configSteps.subModule}/target
                        cp -r ${pathBase}/${pathCode}/${pathCode}/${configSteps.subModule}/target/* ${pathBase}/${pathPackage}/
                    """
            }
        }
    }
}
