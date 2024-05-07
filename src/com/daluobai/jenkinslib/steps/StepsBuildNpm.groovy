package com.daluobai.jenkinslib.steps

import cn.hutool.core.lang.Assert
import cn.hutool.core.util.ObjectUtil
@Grab('cn.hutool:hutool-all:5.8.11')

import cn.hutool.core.util.StrUtil
import com.daluobai.jenkinslib.utils.ConfigUtils
import com.daluobai.jenkinslib.utils.FileUtils
/**
 * @author daluobai@outlook.com
 * version 1.0.0
 * @title 
 * @description https://github.com/daluobai-devops/jenkins-shared-library
 * @create 2023/4/25 12:10
 */
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
        Assert.notNull(configSteps, "DEPLOY_PIPELINE.stepsBuildNpm为空")

        def pathBase = "${steps.env.WORKSPACE}"
        //docker-构建产物目录
        def pathPackage = "package"
        //docker-代码目录
        def pathCode = "code"
        //存放临时sshkey的目录
        def pathSSHKey = "sshkey"

        steps.sh "mkdir -p ${steps.env.WORKSPACE}/${pathPackage}"
        steps.sh "mkdir -p ${steps.env.WORKSPACE}/${pathCode}"
        steps.sh "mkdir -p ${steps.env.WORKSPACE}/${pathSSHKey}"

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

            //容器中缓存modules文件夹的根路径
            def dockerModulesPath = "/path/jenkins/cache/npmModules"
            //容器中缓存modules文件夹的项目路径
            def dockerModulesProjectPath = "${dockerModulesPath}/${steps.currentBuild.projectName}"
            //这里默认会把工作空间挂载到容器中的${steps.env.WORKSPACE}目录
            mavenImage.inside("--entrypoint '' -v npm-repo:${dockerModulesPath}") {
                //从 jenkins 凭据管理中获取密钥文件路径并且拷贝到~/.ssh/id_rsa
                stepsGit.saveJenkinsSSHKey('ssh-git',"${steps.env.WORKSPACE}/${pathSSHKey}/ssh-git")
                //生成known_hosts
                stepsGit.sshKeyscan("${configSteps.gitUrl}", "~/.ssh/known_hosts")
                //不使用缓存node_modules
                if (ObjectUtil.isNotEmpty(configSteps["cacheNodeModules"]) && !configSteps["cacheNodeModules"]){
                    steps.sh "rm -rf ${dockerModulesProjectPath}/node_modules || true"
                }
                steps.sh """
                        #! /bin/sh -e
                        mkdir -p ${pathBase}/${pathPackage} && mkdir -p ${pathBase}/${pathCode} && mkdir -p ${dockerModulesProjectPath}
                        cd ${pathBase}/${pathCode}
                        git config --global http.version HTTP/1.1
                        GIT_SSH_COMMAND='ssh -i ${steps.env.WORKSPACE}/${pathSSHKey}/ssh-git/id_rsa' git clone ${configSteps.gitUrl} --branch ${configSteps.gitBranch} --single-branch --depth 1 --quiet
                        mv ${pathBase}/${pathCode}/\$(ls -A1 ${pathBase}/${pathCode}/) ${pathBase}/${pathCode}/${pathCode}
                        cd ${pathBase}/${pathCode}/${pathCode}
                        git log --pretty=format:"%h -%an,%ar : %s" -1
                        git config core.ignorecase false
                        \\cp -rf ${dockerModulesProjectPath}/node_modules/ . || true
                        ls -al ./node_modules/ || true
                        ${configSteps.buildCMD}
                        ls -al ${pathBase}/${pathCode}/${pathCode}/dist
                        tar -czvf ${pathBase}/${pathPackage}/app.tar.gz -C ${pathBase}/${pathCode}/${pathCode}/dist .
                        rm -rf ${dockerModulesProjectPath}/node_modules || true
                        \\cp -rf ./node_modules ${dockerModulesProjectPath}/ || true
                    """
            }
        }
    }
}
