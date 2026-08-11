package com.daluobai.jenkinslib.delivery

import com.daluobai.jenkinslib.utils.MessageUtils
import com.daluobai.jenkinslib.steps.StepsJenkins

class LegacyJenkinsDeliveryStageAdapter implements DeliveryStageAdapter {
    private final def steps
    private final JenkinsDeliveryStageAdapter delegate

    LegacyJenkinsDeliveryStageAdapter(def steps) {
        this.steps = steps
        this.delegate = new JenkinsDeliveryStageAdapter(steps)
    }

    @Override
    boolean nodesAvailable(Collection nodes) {
        return delegate.nodesAvailable(nodes)
    }

    @Override
    void initializeWorkspace(Map effectiveConfig) {
        // 旧入口历史上只在结束时清理；兼容契约保持不变。
    }

    @Override
    Map build(Map effectiveConfig, Map preflight) {
        return delegate.buildLegacy(effectiveConfig, preflight)
    }

    @Override
    Map store(Map effectiveConfig, Map preflight, Map artifact) {
        Map delivery = effectiveConfig.DELIVERY as Map
        Map storage = delivery.stages.storage as Map
        Map legacyConfig = [
                DEFAULT_CONFIG : effectiveConfig.DEFAULT_CONFIG ?: [:],
                SHARE_PARAM    : [
                        appName    : delivery.application.id,
                        archiveName: delivery.stages.build.artifact?.fileName,
                        message    : delivery.notification?.message
                ],
                DEPLOY_PIPELINE: [stepsStorage: storage.config ?: [:]]
        ]
        steps.globalParameterMap = legacyConfig
        new StepsJenkins(steps).stash((storage.config ?: [:]) as Map)
        String archiveName = legacyConfig.SHARE_PARAM.archiveName?.toString()
        Map stored = new LinkedHashMap(artifact)
        if (archiveName) {
            stored.fileName = archiveName
            stored.path = "package/${archiveName}"
        }
        // 物理存储已由旧 StepsJenkins 完成，不再由新 ArtifactStore 重复执行。
        stored.storageTarget = null
        return stored
    }

    @Override
    void deploy(Map effectiveConfig, Map preflight, Map artifact) {
        delegate.deploy(effectiveConfig, preflight, artifact)
    }

    @Override
    void notify(Map effectiveConfig, String status, Map result) {
        Map delivery = effectiveConfig.DELIVERY as Map
        Map notification = (delivery.notification ?: [:]) as Map
        String applicationId = delivery.application.id.toString()
        String title
        String content
        if (status == 'STARTED') {
            title = "发布开始：${applicationId}"
            content = "发布开始: ${steps.currentBuild.fullDisplayName}"
            new MessageUtils(steps).sendMessage(false, notification.message, title, content)
            return
        }
        if (status == 'ABORTED') {
            return
        }
        title = status == 'SUCCESS' ? "成功:${applicationId}" : "失败:${applicationId}"
        content = status == 'SUCCESS'
                ? "发布成功: ${steps.currentBuild.fullDisplayName}"
                : "发布失败: ${steps.currentBuild.fullDisplayName},异常信息: ${result.failure?.message},构建日志:(${steps.BUILD_URL}console)"
        boolean javaApplication = delivery.application.type == 'JAVA'
        if (javaApplication) {
            new MessageUtils(steps).sendMessage(true, notification.message, title, content)
        } else {
            new MessageUtils(steps).sendMessage(notification.message, title, content)
        }
    }

    @Override
    void cleanupWorkspace(Map effectiveConfig) {
        steps.deleteDir()
    }
}
