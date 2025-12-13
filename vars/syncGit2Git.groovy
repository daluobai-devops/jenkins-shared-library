@GrabResolver(name='aliyun', root='https://maven.aliyun.com/repository/public')
@Grab('cn.hutool:hutool-all:5.8.42')
@Grab('com.typesafe:config:1.4.2')
import cn.hutool.core.lang.Assert
import cn.hutool.core.util.StrUtil
import com.daluobai.jenkinslib.constant.EBuildStatusType
import com.daluobai.jenkinslib.constant.EFileReadType
import com.daluobai.jenkinslib.steps.StepsBuildNpm
import com.daluobai.jenkinslib.steps.StepsGit
import com.daluobai.jenkinslib.steps.StepsJenkins
import com.daluobai.jenkinslib.steps.StepsWeb
import com.daluobai.jenkinslib.utils.ConfigUtils
import com.daluobai.jenkinslib.utils.MapUtils
import cn.hutool.core.util.ObjectUtil
import com.daluobai.jenkinslib.utils.MessageUtils

/**
 * @author daluobai@outlook.com
 * version 1.0.0
 * @title
 * @description https://github.com/daluobai-devops/jenkins-shared-library
 * @create 2023/4/25 12:10
 */
def call(Map customConfig) {

    /*******************初始化全局对象 开始*****************/
    def stepsBuildNpm = new StepsBuildNpm(this)
    def stepsJenkins = new StepsJenkins(this)
    def stepsWeb = new StepsWeb(this)
    def messageUtils = new MessageUtils(this)
    def stepsGit = new StepsGit(this)
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
    /***初始化参数 结束**/
    //默认在同一个构建节点运行，如果需要在其他节点运行则单独写在node块中
    node(nodeBuildNodeList[0]) {
        try {
            //获取并合并配置
            stage("${it}") {
                echo "开始执行流程: ${it}"
                stepsGit.syncGit2Git(customConfig["orgGitUrl"], customConfig["orgCredentialsId"], customConfig["targetGitUrl"], customConfig["targetCredentialsId"])
            }
            echo "结束执行流程: ${it}"
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
                    messageUtils.sendMessage(customConfig.SHARE_PARAM.message, messageTitle, messageContent)
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
    return MapUtils.deepCopy(fullConfig)
}

