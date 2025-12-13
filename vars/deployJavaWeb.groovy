@GrabResolver(name='aliyun', root='https://maven.aliyun.com/repository/public')
@Grab('cn.hutool:hutool-all:5.8.42')
@Grab('com.typesafe:config:1.4.2')
import cn.hutool.core.util.StrUtil
import cn.hutool.core.lang.Assert
import com.daluobai.jenkinslib.constant.EBuildStatusType
import com.daluobai.jenkinslib.constant.EFileReadType
import com.daluobai.jenkinslib.utils.ConfigUtils
import com.daluobai.jenkinslib.utils.MapUtils
import cn.hutool.core.util.ObjectUtil
import com.daluobai.jenkinslib.steps.*
import com.daluobai.jenkinslib.utils.MessageUtils
import com.typesafe.config.*
import groovy.transform.Field

@Field Map globalParameterMap = [:]

/**
 * @author daluobai@outlook.com
 * version 1.0.0
 * @title
 * @description https://github.com/daluobai-devops/jenkins-shared-library
 * @create 2023/4/25 12:10
 */
def call(Map customConfig) {

    /*******************初始化全局对象 开始*****************/
    def stepsBuildMaven = new StepsBuildMaven(this)
    def stepsJenkins = new StepsJenkins(this)
    def stepsJavaWeb = new StepsJavaWeb(this)
    def configUtils = new ConfigUtils(this)
    def messageUtils = new MessageUtils(this)
    def stepsTomcat = new StepsTomcat(this)
    def stepsDeploy = new StepsDeploy(this)
    /*******************初始化全局对象 结束*****************/
    //用来运行构建的节点
    def nodeBuildNodeList = stepsJenkins.getNodeByLabel("buildNode")
    echo "获取到节点:${nodeBuildNodeList}"
    if (ObjectUtil.isEmpty(nodeBuildNodeList)) {
        error '没有可用的构建节点'
    }

    /***初始化参数 开始**/
    //错误信息
    def errMessage = ""
    EBuildStatusType eBuildStatusType = EBuildStatusType.FAILED
    //DEPLOY_PIPELINE顺序定义
    def deployPipelineIndex = ["stepsBuild", "stepsStorage", "stepsDeploy"]
    //如果没传项目名称，则使用jenkins项目名称
    if (StrUtil.isBlank(customConfig.SHARE_PARAM.appName)) {
        customConfig.SHARE_PARAM.appName = currentBuild.projectName
    }
    def SHARE_PARAM = customConfig.SHARE_PARAM
    /***初始化参数 结束**/
    //默认在同一个构建节点运行，如果需要在其他节点运行则单独写在node块中
    node(nodeBuildNodeList[0]) {
        try {
            //获取并合并配置
            def fullConfig = mergeConfig(customConfig)
            echo "fullConfig: ${fullConfig.toString()}"
            //设置共享参数。
            globalParameterMap = fullConfig

            messageUtils.sendMessage(false,customConfig.SHARE_PARAM.message, "发布开始：${customConfig.SHARE_PARAM.appName}", "发布开始: ${currentBuild.fullDisplayName}")

            //执行流程
            deployPipelineIndex.each {
                stage("${it}") {
                    def pipelineConfigItemMap = fullConfig.DEPLOY_PIPELINE[it]
                    if (pipelineConfigItemMap["enable"] != null && pipelineConfigItemMap["enable"] == false) {
                        echo "跳过流程: ${it}"
                        return
                    }
                    echo "开始执行流程: ${it}"
                    if (it == "stepsBuild") {
                        //设置环境变量
                        def stepsBuildEnvList = []
                        if (fullConfig.DEPLOY_PIPELINE.stepsBuild.stepsBuildEnv){
                            stepsBuildEnvList = fullConfig.DEPLOY_PIPELINE.stepsBuild.stepsBuildEnv.collect { k, v -> "${k}=${v}" }
                        }
                        withEnv(stepsBuildEnvList) {
                            stepsBuildMaven.build(fullConfig)
                        }
                    } else if (it == "stepsStorage") {
                        if (ObjectUtil.isEmpty(pipelineConfigItemMap)) {
                            error "stepsStorage配置为空"
                        }
                        stepsJenkins.stash(pipelineConfigItemMap)
                    } else if (it == "stepsDeploy") {
                        messageUtils.sendMessage(false,customConfig.SHARE_PARAM.message, "准备重启：${customConfig.SHARE_PARAM.appName}", "准备重启: ${currentBuild.fullDisplayName}")
                        stepsDeploy.deploy(pipelineConfigItemMap)
                    }
                }
                echo "结束执行流程: ${it}"
            }
            eBuildStatusType = EBuildStatusType.SUCCESS
        } catch (Exception e) {
            if (e instanceof org.jenkinsci.plugins.workflow.steps.FlowInterruptedException) {
                eBuildStatusType = EBuildStatusType.ABORTED
            } else {
                eBuildStatusType = EBuildStatusType.FAILED
                errMessage = e.getMessage()
            }
            throw e
        } finally {
            if (ObjectUtil.isNotEmpty(customConfig.SHARE_PARAM.message)) {
                def messageTitle = ""
                def messageContent = ""
                if (eBuildStatusType == EBuildStatusType.SUCCESS) {
                    messageTitle = "成功:${customConfig.SHARE_PARAM.appName}"
                    messageContent = "发布成功: ${currentBuild.fullDisplayName}"
                } else if (eBuildStatusType == EBuildStatusType.FAILED) {
                    messageTitle = "失败:${customConfig.SHARE_PARAM.appName}"
                    messageContent = "发布失败: ${currentBuild.fullDisplayName},异常信息: ${errMessage},构建日志:(${BUILD_URL}console)"
                } else if (eBuildStatusType == EBuildStatusType.ABORTED) {
                    //发布终止
                }
                if (StrUtil.isNotBlank(messageTitle) && StrUtil.isNotBlank(messageContent)) {
                    messageUtils.sendMessage(true,customConfig.SHARE_PARAM.message, messageTitle, messageContent)
                }
            }
            deleteDir()
        }
    }
}

//获取默认配置路径
def defaultConfigPath(EFileReadType eConfigType) {
    Assert.notNull(eConfigType, "配置类型为空")
    def configPath = null
    if (eConfigType == EFileReadType.HOST_PATH) {
        configPath = "/usr/local/workspace/config/jenkins-pipeline/jenkins-pipeline-config/config.json"
    } else if (eConfigType == EFileReadType.RESOURCES) {
        configPath = "config/config.json"
    } else {
        throw new Exception("暂无默认配置类型")
    }
    return configPath
}

//合并配置customConfig >> extendConfig >> defaultConfig = fullConfig
def mergeConfig(Map customConfig) {

    def fullConfig = [:]
    def extendConfig = [:]
    def defaultConfig = [:]
    //读取默认配置文件
    defaultConfig = new ConfigUtils(this).readConfig(EFileReadType.RESOURCES, defaultConfigPath(EFileReadType.RESOURCES))
    echo "customConfig: ${customConfig.toString()}"
    echo "defaultConfig: ${defaultConfig.toString()}"
    //读取继承配置文件
    if (ObjectUtil.isNotEmpty(customConfig.CONFIG_EXTEND) && ObjectUtil.isNotEmpty(EFileReadType.get(customConfig.CONFIG_EXTEND.configFullPath))) {
        extendConfig = new ConfigUtils(this).readConfigFromFullPath(customConfig.CONFIG_EXTEND.configFullPath)
        echo "extendConfig: ${extendConfig.toString()}"
    }
    //合并自定义配置
    fullConfig = MapUtils.merge([defaultConfig, extendConfig, customConfig])

    //根据自定义构建参数，修改配置
    Config fullConfigParams = ConfigFactory.parseMap(fullConfig)
    echo "fullConfigParams: ${fullConfigParams.toString()}"
    params.each {
        fullConfigParams = fullConfigParams.withValue(it.key, ConfigValueFactory.fromAnyRef(it.value))
    }

    echo "fullConfigParams2: ${fullConfigParams.toString()}"

    fullConfig = fullConfigParams.root().unwrapped()

    echo "fullConfigParams3: ${fullConfig}"
    compatibleConfig(fullConfig)
    echo "fullConfigParams4: ${fullConfig}"
    return MapUtils.deepCopy(fullConfig)
}

//兼容旧的配置
def compatibleConfig(Map customConfig) {
    if (customConfig.DEPLOY_PIPELINE.stepsBuildMaven){
        customConfig.DEPLOY_PIPELINE.stepsBuild = ["stepsBuildMaven":[],"enable":false]
        customConfig.DEPLOY_PIPELINE.stepsBuild.stepsBuildMaven = customConfig.DEPLOY_PIPELINE.stepsBuildMaven
        customConfig.DEPLOY_PIPELINE.stepsBuild.enable = true
    }
    return customConfig
}


