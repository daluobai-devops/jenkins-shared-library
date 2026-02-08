package com.daluobai.jenkinslib.steps

import com.daluobai.jenkinslib.utils.StrUtils
import com.daluobai.jenkinslib.utils.AssertUtils
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
        def configSteps = configMap.DEPLOY_PIPELINE.stepsBuild.stepsBuildMaven

        AssertUtils.notNull(configDefault, "DEFAULT_CONFIG为空")
        AssertUtils.notNull(configShare, "SHARE_PARAM为空")
        AssertUtils.notNull(configSteps, "DEPLOY_PIPELINE.stepsBuildMaven为空")

        def pathBase = "${steps.env.WORKSPACE}"
        //docker-构建产物目录
        def pathPackage = "package"
        //docker-代码目录
        def pathCode = "code"
        //存放临时sshkey的目录
        def pathSSHKey = "sshkey"

        steps.checkout([
                $class: 'GitSCM',
                branches: [[name: '*/master']],
                userRemoteConfigs: [[credentialsId: 'ssh-git', url: 'git@codeup.aliyun.com:.../jgzly-backend.git']],
                extensions: [
                        [$class: 'CloneOption', shallow: false, noTags: false, timeout: 20],
                        [$class: 'PruneStaleBranch'],
                        [$class: 'CleanBeforeCheckout']
                ]
        ])


        steps.sh "mkdir -p ${steps.env.WORKSPACE}/${pathPackage}"
        steps.sh "mkdir -p ${steps.env.WORKSPACE}/${pathCode}"
        steps.sh "mkdir -p ${steps.env.WORKSPACE}/${pathSSHKey}"

        def dockerBuildImage = StrUtils.isNotBlank(configSteps.dockerBuildImage) ? configSteps.dockerBuildImage : "registry.cn-hangzhou.aliyuncs.com/wuzhaozhongguo/build-maven:3-jdk8"
        def dockerBuildImageUrl = "${dockerBuildImage}"

        //获取settings.xml配置，如果没有设置则为空
        def settingsXmlStr = StrUtils.isNotBlank(configSteps.settingsFullPath) ? fileUtils.readStringFromFullPath(configSteps.settingsFullPath) : null

        //如果没有提供登录密钥则不登录
        def dockerLoginDomain = StrUtils.isNotBlank(configDefault.docker.registry.credentialsId) ? "https://${configDefault.docker.registry.domain}" : ""
        def dockerLoginCredentialsId = StrUtils.isNotBlank(configDefault.docker.registry.credentialsId) ? configDefault.docker.registry.credentialsId : ""

        steps.withDockerRegistry(credentialsId: dockerLoginCredentialsId, url: dockerLoginDomain) {
            steps.sh "whoami"
            def mavenImage = steps.docker.image("${dockerBuildImageUrl}")
            mavenImage.pull()

            def mvnCMDSubMod = "-pl ${configSteps.subModule} -am -amd"
            def mvnCMDActiveProfile = StrUtils.isNotEmpty(configSteps.activeProfile) ? "-P ${configSteps.activeProfile}" : ""

            //这里默认会把工作空间挂载到容器中的${steps.env.WORKSPACE}目录
            mavenImage.inside("--entrypoint '' -v maven-repo:/root/.m2/repository") {
                //从 jenkins 凭据管理中获取密钥文件路径并且拷贝到工作目录下的ssh-git目录，后面clone的时候指定密钥为这个
                stepsGit.saveJenkinsSSHKey('ssh-git',"${steps.env.WORKSPACE}/${pathSSHKey}/ssh-git")
                //生成known_hosts
                stepsGit.sshKeyscan("${configSteps.gitUrl}", "~/.ssh/known_hosts")
                //如果有settings.xml配置则写入用户自定义配置.
                if (StrUtils.isNotBlank(settingsXmlStr)){
                    fileUtils.writeFileBySH("~/.m2/settings.xml", settingsXmlStr)
                }
                steps.sh """
                        #! /bin/sh -e
                        mkdir -p ${pathBase}/${pathPackage} && mkdir -p ${pathBase}/${pathCode}
                        cd ${pathBase}/${pathCode} && rm -rf *
                        git config --global http.version HTTP/1.1
                        GIT_SSH_COMMAND='ssh -i ${steps.env.WORKSPACE}/${pathSSHKey}/ssh-git/id_rsa' git clone ${configSteps.gitUrl} --branch ${configSteps.gitBranch} --single-branch --depth 1 --quiet
                        mv ${pathBase}/${pathCode}/\$(ls -A1 ${pathBase}/${pathCode}/) ${pathBase}/${pathCode}/${pathCode}
                        cd ${pathBase}/${pathCode}/${pathCode}
                        git log --pretty=format:"%h -%an,%ar : %s" -1
                        git config core.ignorecase false
                        mvn -Dmaven.test.skip=${configSteps.skipTest} ${configSteps.lifecycle} -Dmaven.compile.fork=true -U -B ${mvnCMDSubMod} ${mvnCMDActiveProfile}
                        ls -al ${pathBase}/${pathCode}/${pathCode}/${configSteps.subModule}/target
                        cp -r ${pathBase}/${pathCode}/${pathCode}/${configSteps.subModule}/target/* ${pathBase}/${pathPackage}/
                    """
            }
        }
    }
}
