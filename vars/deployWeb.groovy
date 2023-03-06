@Grab('cn.hutool:hutool-all:5.8.11')
import cn.hutool.core.lang.Assert
import cn.hutool.core.util.StrUtil
import com.daluobai.jenkinslib.constant.EFileReadType
import com.daluobai.jenkinslib.constant.GlobalShare
import com.daluobai.jenkinslib.steps.StepsBuildNpm
import com.daluobai.jenkinslib.steps.StepsJenkins
import com.daluobai.jenkinslib.steps.StepsJavaWeb
import com.daluobai.jenkinslib.utils.ConfigUtils
import com.daluobai.jenkinslib.utils.MapUtils
import cn.hutool.core.util.ObjectUtil


def call(Map customConfig) {

    /*******************初始化全局对象 开始*****************/
    def stepsBuildNpm = new StepsBuildNpm(this)
    def stepsJenkins = new StepsJenkins(this)
    def stepsJavaWeb = new StepsJavaWeb(this)
    def configUtils = new ConfigUtils(this)
    /*******************初始化全局对象 结束*****************/
    //用来运行构建的节点
    def nodeBuildNodeList = stepsJenkins.getNodeByLabel("buildNode")
    echo "获取到节点:${nodeBuildNodeList}"
    if (ObjectUtil.isEmpty(nodeBuildNodeList)) {
        error '没有可用的构建节点'
    }
    //初始化参数.
    //如果没传项目名称，则使用jenkins项目名称
    if (StrUtil.isBlank(customConfig.SHARE_PARAM.appName)){
        customConfig.SHARE_PARAM.appName = currentBuild.projectName
    }
    def SHARE_PARAM =  customConfig.SHARE_PARAM
    //设置共享参数。
    GlobalShare.globalParameterMap = SHARE_PARAM
    //默认在同一个构建节点运行，如果需要在其他节点运行则单独写在node块中
    node(nodeBuildNodeList[0]) {
        try {
            //获取并合并配置
            def fullConfig = mergeConfig(customConfig)
            echo "fullConfig: ${fullConfig.toString()}"
            //执行流程
            fullConfig["DEPLOY_PIPELINE"].each {
                stage("${it.key}") {
                    if (it.value["enable"] != null && it.value["enable"] == false) {
                        echo "跳过流程: ${it.key}"
                        return
                    }
                    echo "开始执行流程: ${it.key}"
                    if (it.key == "stepsBuildNpm") {
                        stepsBuildNpm.build(fullConfig)
                    } else if (it.key == "stepsStorage") {
                        stepsJenkins.stash(fullConfig.DEPLOY_PIPELINE.stepsStorage)
                    } else if (it.key == "stepsJavaWebDeployToWebServer") {
//                        stepsJavaWeb.deploy(fullConfig.DEPLOY_PIPELINE.stepsJavaWebDeployToWebServer)
                    }
                }
                echo "结束执行流程: ${it.key}"
            }
        } catch (Exception e) {
            echo "执行异常: ${e.toString()}"
            currentBuild.result = "FAILURE"
            throw e
        } finally {
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
    }  else {
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

    return MapUtils.deepCopy(fullConfig)
}

