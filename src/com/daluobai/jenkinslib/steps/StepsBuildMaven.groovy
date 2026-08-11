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
        return buildInternal(configMap, null)
    }

    // 当前统一交付入口：构建已经由源码适配器检出的唯一工作副本。
    def buildFromSource(Map configMap, String workingCopyRoot = 'source') {
        AssertUtils.notBlank(workingCopyRoot, 'workingCopyRoot为空')
        return buildInternal(configMap, workingCopyRoot)
    }

    private def buildInternal(Map configMap, String workingCopyRoot) {
        //默认配置
        def configDefault = configMap["DEFAULT_CONFIG"]//默认配置
        //共享配置
        def configShare = configMap["SHARE_PARAM"]
        //流程配置
        def configSteps = configMap.DEPLOY_PIPELINE.stepsBuild.stepsBuildMaven

        AssertUtils.notNull(configDefault, "DEFAULT_CONFIG为空")
        AssertUtils.notNull(configShare, "SHARE_PARAM为空")
        AssertUtils.notNull(configSteps, "DEPLOY_PIPELINE.stepsBuildMaven为空")
        if (workingCopyRoot == null) {
            AssertUtils.notBlank(configSteps.gitUrl?.toString(), "gitUrl为空")
            AssertUtils.notBlank(configSteps.gitBranch?.toString(), "gitBranch为空")
        }
        AssertUtils.notBlank(configSteps.lifecycle?.toString(), "lifecycle为空")

        def pathBase = "${steps.env.WORKSPACE}"
        //docker-构建产物目录
        def pathPackage = "package"
        //docker-代码目录
        def pathCode = "code"
        //存放临时sshkey的目录
        def pathSSHKey = "sshkey"

//        steps.checkout([
//                $class: 'GitSCM',
//                branches: [[name: '*/upgrade']],
//                userRemoteConfigs: [[credentialsId: 'ssh-git', url: 'git@codeup.aliyun.com:5fef130e578739320804fdd8/company/njzs/jgzly/jgzly-backend.git']],
//                extensions: [
//                        [$class: 'CloneOption', shallow: false, noTags: false, timeout: 20],
//                        [$class: 'PruneStaleBranch'],
//                        [$class: 'CleanBeforeCheckout']
//                ]
//        ])


        steps.sh "mkdir -p ${steps.env.WORKSPACE}/${pathPackage}"
        if (workingCopyRoot == null) {
            steps.sh "mkdir -p ${steps.env.WORKSPACE}/${pathCode}"
            steps.sh "mkdir -p ${steps.env.WORKSPACE}/${pathSSHKey}"
        }

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

            boolean hasSubModule = StrUtils.isNotBlank(configSteps.subModule?.toString())
            def mvnCMDSubMod = hasSubModule ? "-pl ${configSteps.subModule} -am -amd" : ""
            def mvnCMDActiveProfile = StrUtils.isNotEmpty(configSteps.activeProfile) ? "-P ${configSteps.activeProfile}" : ""
            String sourceDirectory = StrUtils.isNotBlank(configSteps.sourceDirectory) ? configSteps.sourceDirectory.toString() : "."
            String repositoryRoot = workingCopyRoot == null ? "${pathBase}/${pathCode}/${pathCode}" : "${pathBase}/${workingCopyRoot}"
            String sourceRoot = sourceDirectory == "." ? repositoryRoot : "${repositoryRoot}/${sourceDirectory}"
            def targetPath = hasSubModule ?
                    "${sourceRoot}/${configSteps.subModule}/target" :
                    "${sourceRoot}/target"

            //这里默认会把工作空间挂载到容器中的${steps.env.WORKSPACE}目录
            mavenImage.inside("--entrypoint '' -v maven-repo:/root/.m2/repository") {
                if (workingCopyRoot == null && isSshGitUrl(configSteps.gitUrl?.toString())) {
                    String credentialsId = StrUtils.isNotBlank(configSteps.credentialsId) ? configSteps.credentialsId : "ssh-git"
                    stepsGit.saveJenkinsSSHKey(credentialsId,"${steps.env.WORKSPACE}/${pathSSHKey}/ssh-git")
                    stepsGit.sshKeyscan("${configSteps.gitUrl}", "~/.ssh/known_hosts")
                }
                //如果有settings.xml配置则写入用户自定义配置.
                if (StrUtils.isNotBlank(settingsXmlStr)){
                    fileUtils.writeFileBySH("~/.m2/settings.xml", settingsXmlStr)
                }
                if (workingCopyRoot == null) {
                    steps.sh """
                        #! /bin/sh -e
                        rm -rf ${pathBase}/${pathCode}/${pathCode}
                        mkdir -p ${pathBase}/${pathPackage} && mkdir -p ${pathBase}/${pathCode}/${pathCode}
                        git config --global http.version HTTP/1.1
                    """
                    // 使用 Jenkins checkout 才会写入 currentBuild.changeSets，构建记录的 Changes 页面才能显示项目提交历史。
                    // checkout 目录保持为 code/code，避免影响旧入口后续 Maven 构建和 target 产物复制路径。
                    steps.dir("${pathBase}/${pathCode}/${pathCode}") {
                        steps.checkout(changelog: true, poll: false, scm: [
                                $class           : 'GitSCM',
                                branches         : [[name: "${configSteps.gitBranch}".toString()]],
                                userRemoteConfigs: [gitUserRemoteConfig(configSteps)],
                                extensions       : [[$class: 'CleanBeforeCheckout']]
                        ])
                    }
                }
                steps.sh """
                        #! /bin/sh -e
                        cd '${sourceRoot}'
                        git log --pretty=format:"%h -%an,%ar : %s" -1
                        git config core.ignorecase false
                        mvn -Dmaven.test.skip=${configSteps.skipTest} ${configSteps.lifecycle} -Dmaven.compile.fork=true -U -B ${mvnCMDSubMod} ${mvnCMDActiveProfile}
                        ls -al '${targetPath}'
                        cp -r '${targetPath}'/* '${pathBase}/${pathPackage}/'
                    """
            }
        }
    }

    // SSH 仓库默认沿用共享库历史约定的 ssh-git 凭据；如需覆盖，可在 stepsBuildMaven.credentialsId 显式指定。
    private Map gitUserRemoteConfig(def configSteps) {
        def remoteConfig = [url: "${configSteps.gitUrl}".toString()]
        def credentialsId = StrUtils.isNotBlank(configSteps.credentialsId) ? configSteps.credentialsId : defaultGitCredentialsId("${configSteps.gitUrl}")
        if (StrUtils.isNotBlank(credentialsId)) {
            remoteConfig.credentialsId = credentialsId
        }
        return remoteConfig
    }

    // HTTPS/HTTP 仓库不强行附带凭据，避免破坏公开仓库或已有匿名访问配置。
    private String defaultGitCredentialsId(String gitUrl) {
        if (StrUtils.isBlank(gitUrl)) {
            return ""
        }
        return gitUrl.startsWith("git@") || gitUrl.startsWith("ssh://") ? "ssh-git" : ""
    }

    private static boolean isSshGitUrl(String gitUrl) {
        return StrUtils.isNotBlank(gitUrl) && (gitUrl.startsWith("git@") || gitUrl.startsWith("ssh://"))
    }
}
