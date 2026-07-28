package com.daluobai.jenkinslib.delivery

import groovy.json.JsonOutput
import groovy.json.JsonSlurperClassic
import com.cloudbees.groovy.cps.NonCPS
import com.daluobai.jenkinslib.utils.MessageUtils

class JenkinsDeliveryExecutionCoordinator implements DeliveryExecutionCoordinator {
    private final def steps

    JenkinsDeliveryExecutionCoordinator(def steps) {
        this.steps = steps
    }

    @Override
    void deploymentStarted(Map execution, Map result) {
        if (steps.currentBuild != null) {
            steps.currentBuild.description = JsonOutput.toJson(new LinkedHashMap(result) + [deploymentStarted: true])
        }
    }

    @Override
    void beforeExecution(Map execution, Map effectiveConfig) {
        Map previous
        while ((previous = previousExecution()) != null) {
            stopPrevious(previous.number as int)
            steps.waitUntil(initialRecurrencePeriod: 1000) {
                return !isBuilding(previous.number as int)
            }
            notifySupersession(effectiveConfig, previous, execution)
        }
    }

    @Override
    void afterExecution(Map execution, Map result) {
        if (steps.currentBuild != null) {
            steps.currentBuild.description = JsonOutput.toJson(result)
            steps.currentBuild.result = result.status == 'SUCCESS' ? 'SUCCESS' : 'FAILURE'
        }
    }

    @NonCPS
    private Map previousExecution() {
        def previous = steps.currentBuild?.rawBuild?.getPreviousBuildInProgress()
        if (previous == null) {
            return null
        }
        Map recorded = [:]
        try {
            recorded = previous.description ? new JsonSlurperClassic().parseText(previous.description.toString()) as Map : [:]
        } catch (Throwable ignored) {
            recorded = [:]
        }
        return [
                number          : previous.number,
                displayName     : previous.displayName?.toString(),
                deploymentStarted: recorded.deploymentStarted == true
        ]
    }

    @NonCPS
    private void stopPrevious(int buildNumber) {
        steps.currentBuild.rawBuild.parent.getBuildByNumber(buildNumber)?.doStop()
    }

    @NonCPS
    private boolean isBuilding(int buildNumber) {
        return steps.currentBuild.rawBuild.parent.getBuildByNumber(buildNumber)?.isBuilding() == true
    }

    private void notifySupersession(Map effectiveConfig, Map previous, Map execution) {
        String applicationId = effectiveConfig.DELIVERY.application.id.toString()
        String environment = effectiveConfig.DELIVERY.targetEnvironment.toString()
        String body = "旧执行=${previous.displayName ?: previous.number}, 新执行=${execution.id ?: steps.env.BUILD_NUMBER}, " +
                "应用=${applicationId}, 环境=${environment}, 旧执行已进入部署=${previous.deploymentStarted}"
        steps.echo("交付替代: ${body}")
        Map notification = (effectiveConfig.DELIVERY.notification ?: [:]) as Map
        if (notification.enabled == true) {
            try {
                new MessageUtils(steps).sendMessage(notification.message as Map, "交付执行被替代：${applicationId}", body)
            } catch (Throwable failure) {
                steps.echo("交付替代通知失败（不阻塞新执行）: ${failure.message}")
            }
        }
    }
}
