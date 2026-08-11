package com.daluobai.jenkinslib.steps

import com.daluobai.jenkinslib.utils.AssertUtils
import com.daluobai.jenkinslib.utils.ObjUtils
import com.daluobai.jenkinslib.utils.StrUtils
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
        //流程配置-构建
        def configSteps = configMap.DEPLOY_PIPELINE.stepsBuildNpm
        //流程配置-存储
        def configStepsStorage = configMap.DEPLOY_PIPELINE.stepsStorage

        AssertUtils.notNull(configDefault, "DEFAULT_CONFIG为空")
        AssertUtils.notNull(configShare, "SHARE_PARAM为空")
        AssertUtils.notNull(configSteps, "DEPLOY_PIPELINE.stepsBuildNpm为空")
        if (workingCopyRoot == null) {
            AssertUtils.notBlank(configSteps.gitUrl?.toString(), "gitUrl为空")
            AssertUtils.notBlank(configSteps.gitBranch?.toString(), "gitBranch为空")
        }
        AssertUtils.notBlank(configSteps.buildCMD?.toString(), "buildCMD为空")
        AssertUtils.isTrue(configStepsStorage?.archiveType in ["TAR", "ZIP"], "stepsStorage.archiveType仅支持TAR或ZIP")

        def pathBase = "${steps.env.WORKSPACE}"
        //docker-构建产物目录
        def pathPackage = "package"
        //docker-代码目录
        def pathCode = "code"
        String sourceDirectory = StrUtils.isNotBlank(configSteps.sourceDirectory) ? configSteps.sourceDirectory.toString() : "."
        String repositoryRoot = workingCopyRoot == null ? "${pathBase}/${pathCode}/${pathCode}" : "${pathBase}/${workingCopyRoot}"
        String sourceRoot = sourceDirectory == "." ? repositoryRoot : "${repositoryRoot}/${sourceDirectory}"
        //存放临时sshkey的目录
        def pathSSHKey = "sshkey"

        steps.sh "mkdir -p ${steps.env.WORKSPACE}/${pathPackage}"
        if (workingCopyRoot == null) {
            steps.sh "mkdir -p ${steps.env.WORKSPACE}/${pathCode}"
            steps.sh "mkdir -p ${steps.env.WORKSPACE}/${pathSSHKey}"
        }

        def dockerBuildImage = StrUtils.isNotBlank(configSteps.dockerBuildImage) ? configSteps.dockerBuildImage : "registry.cn-hangzhou.aliyuncs.com/wuzhaozhongguo/build-npm:10.16.0"
        def dockerBuildImageUrl = "${dockerBuildImage}"

        //如果没有提供登录密钥则不登录
        def dockerLoginDomain = StrUtils.isNotBlank(configDefault.docker.registry.credentialsId) ? "https://${configDefault.docker.registry.domain}" : ""
        def dockerLoginCredentialsId = StrUtils.isNotBlank(configDefault.docker.registry.credentialsId) ? configDefault.docker.registry.credentialsId : ""

        steps.withDockerRegistry(credentialsId: dockerLoginCredentialsId, url: dockerLoginDomain) {

            def mavenImage = steps.docker.image("${dockerBuildImageUrl}")
            mavenImage.pull()

            //容器中缓存modules文件夹的根路径
            def dockerModulesPath = "/root/.npm"
            //容器中缓存modules文件夹的项目路径
            def dockerModulesProjectPath = "${dockerModulesPath}/${steps.currentBuild.projectName}"
            boolean cacheNodeModules = configSteps.cacheNodeModules != false
            String restoreNodeModulesCMD = cacheNodeModules ? """
                        rm -rf node_modules
                        if [ -d ${dockerModulesProjectPath}/node_modules ]; then
                            cp -a ${dockerModulesProjectPath}/node_modules ./node_modules
                        fi
                    """ : "rm -rf ${dockerModulesProjectPath}/node_modules node_modules"
            String persistNodeModulesCMD = cacheNodeModules ? """
                        rm -rf ${dockerModulesProjectPath}/node_modules
                        if [ -d node_modules ]; then
                            mkdir -p ${dockerModulesProjectPath}
                            cp -a node_modules ${dockerModulesProjectPath}/node_modules
                        fi
                    """ : ":"
            String archiveCommand = configStepsStorage.archiveType == "ZIP" ? """
                        cd '${sourceRoot}/dist'
                        zip -r ${pathBase}/${pathPackage}/app.zip .
                    """ : "tar -czvf '${pathBase}/${pathPackage}/app.tar.gz' -C '${sourceRoot}/dist' ."
            //这里默认会把工作空间挂载到容器中的${steps.env.WORKSPACE}目录
            mavenImage.inside("--entrypoint '' -v npm-repo:${dockerModulesPath}") {
                if (workingCopyRoot == null && isSshGitUrl(configSteps.gitUrl?.toString())) {
                    String credentialsId = StrUtils.isNotBlank(configSteps.credentialsId) ? configSteps.credentialsId : "ssh-git"
                    stepsGit.saveJenkinsSSHKey(credentialsId,"${steps.env.WORKSPACE}/${pathSSHKey}/ssh-git")
                    stepsGit.sshKeyscan("${configSteps.gitUrl}", "~/.ssh/known_hosts")
                }
                if (workingCopyRoot == null) {
                    steps.sh """
                        #! /bin/sh -e
                        rm -rf ${pathBase}/${pathCode}/${pathCode}
                        mkdir -p ${pathBase}/${pathPackage} && mkdir -p ${pathBase}/${pathCode}/${pathCode} && mkdir -p ${dockerModulesProjectPath}
                        git config --global http.version HTTP/1.1
                    """
                    // 使用 Jenkins checkout 才会写入 currentBuild.changeSets，构建记录的 Changes 页面才能显示项目提交历史。
                    // checkout 目录保持为 code/code，避免影响旧入口后续 buildCMD、dist 检查和打包路径。
                    steps.dir("${pathBase}/${pathCode}/${pathCode}") {
                        steps.checkout(changelog: true, poll: false, scm: [
                                $class           : 'GitSCM',
                                branches         : [[name: "${configSteps.gitBranch}".toString()]],
                                userRemoteConfigs: [gitUserRemoteConfig(configSteps)],
                                extensions       : [[$class: 'CleanBeforeCheckout']]
                        ])
                    }
                } else {
                    steps.sh "mkdir -p ${pathBase}/${pathPackage} && mkdir -p ${dockerModulesProjectPath}"
                }
                steps.sh """
                        #! /bin/sh -e
                        cd '${sourceRoot}'
                        git log --pretty=format:"%h -%an,%ar : %s" -1
                        git config core.ignorecase false
                        ${restoreNodeModulesCMD}
                        ${configSteps.buildCMD}
                        ${persistNodeModulesCMD}
                        ls -al '${sourceRoot}/dist'
                        ${archiveCommand}
                    """
            }
        }
    }

    // SSH 仓库默认沿用共享库历史约定的 ssh-git 凭据；如需覆盖，可在 stepsBuildNpm.credentialsId 显式指定。
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
