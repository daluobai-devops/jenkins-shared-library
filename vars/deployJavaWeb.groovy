@Grab('cn.hutool:hutool-all:5.8.11')
import cn.hutool.core.lang.Assert
import com.daluobai.jenkinslib.constant.EConfigType
import com.daluobai.jenkinslib.constant.GlobalShare
import com.daluobai.jenkinslib.steps.StepsBuildMaven
import com.daluobai.jenkinslib.steps.StepsJenkins
import com.daluobai.jenkinslib.steps.StepsJavaWeb
import com.daluobai.jenkinslib.utils.ConfigUtils
import com.daluobai.jenkinslib.utils.MapUtils
import cn.hutool.core.util.ObjectUtil

def call(Map customConfig) {

    /*******************初始化全局对象 开始*****************/
    def stepsBuildMaven = new StepsBuildMaven(this)
    def stepsJenkins = new StepsJenkins(this)
    def stepsJavaWeb = new StepsJavaWeb(this)
    /*******************初始化全局对象 结束*****************/
    //用来运行构建的节点
    def nodeBuildNodeList = stepsJenkins.getNodeByLabel("buildNode")
    echo "获取到节点:${nodeBuildNodeList}"
    if (ObjectUtil.isEmpty(nodeBuildNodeList)) {
        error '没有可用的构建节点'
    }
    def SHARE_PARAM =  customConfig.SHARE_PARAM
    //设置共享参数
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
                    if (it.key == "stepsBuildMaven") {
                        stepsBuildMaven.build(fullConfig)
                    } else if (it.key == "stepsStorage") {
                        stepsJenkins.stash(fullConfig.DEPLOY_PIPELINE.stepsStorage)
                    } else if (it.key == "stepsJavaWebDeploy") {
                        stepsJavaWeb.deploy(fullConfig.DEPLOY_PIPELINE.stepsJavaWebDeploy)
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
def defaultConfigPath(EConfigType eConfigType) {
    Assert.notNull(eConfigType, "配置类型为空")
    def configPath = null
    if (eConfigType == EConfigType.HOST_PATH) {
        configPath = "/usr/local/workspace/config/jenkins-pipeline/jenkins-pipeline-config/config.json"
    } else if (eConfigType == EConfigType.RESOURCES) {
        configPath = "config/config.groovy"
    } else if (eConfigType == EConfigType.URL) {
        configPath = "http://xxxxx"
    } else {
        throw new Exception("暂不支持的配置类型")
    }
    return configPath
}

//合并配置customConfig >> extendConfig >> defaultConfig = fullConfig
def mergeConfig(Map customConfig) {

    def fullConfig = [:]
    def extendConfig = [:]
    def defaultConfig = [:]
    //读取默认配置文件
    defaultConfig = ConfigUtils.readConfigFromResource(this, defaultConfigPath(EConfigType.RESOURCES))
    echo "customConfig: ${customConfig.toString()}"
    echo "defaultConfig: ${defaultConfig.getClass()}"
    //读取继承配置文件
    if (ObjectUtil.isNotEmpty(customConfig.CONFIG_EXTEND) && ObjectUtil.isNotEmpty(EConfigType.get(customConfig.CONFIG_EXTEND.configType)) && ObjectUtil.isNotEmpty(customConfig.CONFIG_EXTEND.path)) {
        EConfigType extendConfigType = EConfigType.get(customConfig.CONFIG_EXTEND.configType)
        extendConfig = ConfigUtils.readConfig(extendConfigType, customConfig.CONFIG_EXTEND.path)
        echo "extendConfig: ${extendConfig.toString()}"
    }
    //合并自定义配置
    fullConfig = MapUtils.merge([defaultConfig, extendConfig, customConfig])

    return MapUtils.deepCopy(fullConfig)
}
