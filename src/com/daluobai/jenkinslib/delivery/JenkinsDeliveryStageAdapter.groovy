package com.daluobai.jenkinslib.delivery

import com.daluobai.jenkinslib.steps.StepsBuildMaven
import com.daluobai.jenkinslib.steps.StepsBuildNpm
import com.daluobai.jenkinslib.steps.StepsDeploy
import com.daluobai.jenkinslib.steps.StepsJenkins
import com.daluobai.jenkinslib.steps.StepsWeb
import com.daluobai.jenkinslib.utils.MessageUtils

class JenkinsDeliveryStageAdapter implements DeliveryStageAdapter {
    private final def steps

    JenkinsDeliveryStageAdapter(def steps) {
        this.steps = steps
    }

    @Override
    boolean nodesAvailable(Collection nodes) {
        StepsJenkins jenkins = new StepsJenkins(steps)
        return nodes.every { Object label ->
            def resolved = jenkins.getNodeByLabel(label.toString())
            return resolved != null && !resolved.isEmpty()
        }
    }

    @Override
    void initializeWorkspace(Map effectiveConfig) {
        steps.deleteDir()
    }

    @Override
    Map build(Map effectiveConfig, Map preflight) {
        Map legacy = strategyConfig(effectiveConfig, preflight)
        Map delivery = effectiveConfig.DELIVERY as Map
        Closure buildAction = {
            if (delivery.application.type == 'JAVA') {
                new StepsBuildMaven(steps).build(legacy)
            } else {
                new StepsBuildNpm(steps).build(legacy)
            }
        }
        buildAction.call()
        Map artifact = new LinkedHashMap((delivery.stages.build.artifact ?: [:]) as Map)
        if (delivery.application.type == 'WEB') {
            Map webStorage = storageConfig(delivery.stages.storage as Map, delivery.stages.deploy as Map, delivery.stages.build as Map)
            if (webStorage.archiveType == 'ZIP') {
                artifact.fileName = 'app.zip'
                artifact.path = 'package/app.zip'
            } else if (webStorage.archiveType == 'TAR') {
                artifact.fileName = 'app.tar.gz'
                artifact.path = 'package/app.tar.gz'
            }
        } else if (delivery.stages.storage?.enabled != true && delivery.stages.deploy?.enabled == true &&
                delivery.stages.deploy.directArtifactHandoff == true) {
            boolean tomcat = delivery.stages.deploy.strategy == 'TOMCAT'
            String archiveName = tomcat ? 'app.war' : 'app.jar'
            String sourcePattern = tomcat ? '*.war' : '*.jar'
            if (!tomcat) {
                steps.sh('rm -f package/*-sources.jar')
            }
            steps.sh("mv package/${sourcePattern} package/${archiveName} || true")
            artifact.fileName = archiveName
            artifact.path = "package/${archiveName}"
        }
        if (!artifact.path) {
            artifact.path = 'package/**'
        }
        artifact.fileName = artifact.fileName ?: artifact.path.toString().tokenize('/').last()
        if (delivery.stages.storage?.enabled == true || delivery.stages.deploy?.enabled == true) {
            artifact.checksum = steps.sh(
                    script: 'find package -type f -print0 | sort -z | xargs -0 sha256sum | sha256sum | awk \'{print $1}\'',
                    returnStdout: true
            ).toString().trim()
        }
        return artifact
    }

    @Override
    Map store(Map effectiveConfig, Map preflight, Map artifact) {
        Map storage = effectiveConfig.DELIVERY.stages.storage as Map
        Map stored = new LinkedHashMap(artifact)
        if (storage.target?.type == 'JENKINS_STASH') {
            String archiveType = storage.config?.archiveType?.toString()?.toUpperCase(Locale.ROOT)
            Map formats = [JAR: ['app.jar', '*.jar'], WAR: ['app.war', '*.war'], TAR: ['app.tar.gz', '*.tar.gz'], ZIP: ['app.zip', '*.zip']]
            if (!formats.containsKey(archiveType)) {
                throw new IllegalArgumentException("JENKINS_STASH不支持产物格式: ${archiveType}")
            }
            String archiveName = formats[archiveType][0]
            String sourcePattern = formats[archiveType][1]
            if (archiveType == 'JAR') {
                steps.sh('rm -f package/*-sources.jar')
            }
            steps.sh("mv package/${sourcePattern} package/${archiveName} || true")
            String includes = "package/${archiveName}"
            steps.stash(name: 'appPackage', includes: includes, useDefaultExcludes: false)
            stored.path = includes
            stored.fileName = archiveName
        }
        stored.storageTarget = storage.target?.type
        return stored
    }

    @Override
    void deploy(Map effectiveConfig, Map preflight, Map artifact) {
        Map delivery = effectiveConfig.DELIVERY as Map
        Map deploy = delivery.stages.deploy as Map
        Map legacy = strategyConfig(effectiveConfig, preflight)
        legacy.SHARE_PARAM.archiveName = artifact.fileName
        Map deploymentConfig = new LinkedHashMap((deploy.config ?: [:]) as Map)
        Map javaServiceConfig = strategyDeploymentConfig(deploymentConfig, 'stepsJavaWebDeployToService')
        Map tomcatConfig = strategyDeploymentConfig(deploymentConfig, 'stepsTomcatDeploy')
        if (effectiveConfig.DELIVERY.stages.storage?.enabled != true && deploy.directArtifactHandoff == true) {
            steps.stash(name: 'appPackage', includes: artifact.path.toString(), useDefaultExcludes: false)
        }
        deploymentConfig.labels = deploy.nodes
        if (deploy.readiness instanceof Collection) {
            deploymentConfig.readinessSequence = deploy.readiness
            Map readinessProbe = [:]
            (deploy.readiness as Collection).each { Object rawCheck ->
                Map check = rawCheck as Map
                String key = [TCP: 'tcp', HTTP: 'http', COMMAND: 'cmd'][check.type?.toString()]
                if (key) {
                    readinessProbe[key] = (check.config ?: [:]) + [enable: true]
                }
            }
            deploymentConfig.readinessProbe = readinessProbe
        }
        if (delivery.application.type == 'JAVA') {
            if (deploy.strategy == 'TOMCAT') {
                deploymentConfig.stepsTomcatDeploy = tomcatConfig + [enable: true]
                deploymentConfig.stepsJavaWebDeployToService = javaServiceConfig + [enable: false]
            } else {
                deploymentConfig.stepsJavaWebDeployToService = javaServiceConfig + [enable: true]
                deploymentConfig.stepsTomcatDeploy = tomcatConfig + [enable: false]
            }
            new StepsDeploy(steps).deploy(deploymentConfig, legacy)
        } else {
            new StepsWeb(steps).deploy(deploymentConfig, legacy)
        }
    }

    static Map strategyDeploymentConfig(Map deploymentConfig, String legacyKey) {
        Object legacyConfig = deploymentConfig[legacyKey]
        if (legacyConfig instanceof Map) {
            return new LinkedHashMap(legacyConfig as Map)
        }
        Map strategyConfig = new LinkedHashMap(deploymentConfig)
        strategyConfig.remove('stepsJavaWebDeployToService')
        strategyConfig.remove('stepsTomcatDeploy')
        return strategyConfig
    }

    @Override
    void notify(Map effectiveConfig, String status, Map result) {
        Map notification = (effectiveConfig.DELIVERY.notification ?: [:]) as Map
        if (notification.enabled != true) {
            return
        }
        String applicationId = effectiveConfig.DELIVERY.application.id.toString()
        String title = status == 'STARTED' ? "交付开始：${applicationId}" : "${status}:${applicationId}"
        new MessageUtils(steps).sendMessage(notification.message as Map, title, result.toString())
    }

    @Override
    void cleanupWorkspace(Map effectiveConfig) {
        steps.deleteDir()
    }

    private static Map strategyConfig(Map effectiveConfig, Map preflight) {
        Map delivery = effectiveConfig.DELIVERY as Map
        Map build = delivery.stages.build as Map
        Map config = [
                DEFAULT_CONFIG : (effectiveConfig.DEFAULT_CONFIG ?: [:]),
                SHARE_PARAM    : [
                        appName    : delivery.application.id,
                        archiveName: build.artifact?.fileName,
                        message    : delivery.notification?.message
                ],
                DEPLOY_PIPELINE: [
                        stepsStorage: storageConfig(delivery.stages.storage as Map, delivery.stages.deploy as Map, build),
                        stepsDeploy : (delivery.stages.deploy.config ?: [:])
                ]
        ]
        Map strategy = new LinkedHashMap((build.config ?: [:]) as Map)
        strategy.gitUrl = delivery.source.repository
        strategy.gitBranch = preflight.resolvedSourceRevision
        strategy.sourceDirectory = delivery.source.directory
        if (delivery.application.type == 'JAVA') {
            config.DEPLOY_PIPELINE.stepsBuild = [enable: true, stepsBuildMaven: strategy]
        } else {
            config.DEPLOY_PIPELINE.stepsBuildNpm = strategy + [enable: true]
        }
        return config
    }

    private static Map storageConfig(Map storage, Map deploy, Map build) {
        Map config = new LinkedHashMap((storage.config ?: [:]) as Map)
        if (!config.archiveType && storage.enabled != true && deploy.enabled == true && deploy.directArtifactHandoff == true) {
            String fileName = build.artifact?.fileName?.toString()?.toLowerCase(Locale.ROOT)
            config.archiveType = fileName?.endsWith('.zip') ? 'ZIP' :
                    ((fileName?.endsWith('.tar') || fileName?.endsWith('.tar.gz')) ? 'TAR' : null)
        }
        if ((storage.enabled == true && storage.target?.type == 'JENKINS_STASH') ||
                (storage.enabled != true && deploy.enabled == true && deploy.directArtifactHandoff == true)) {
            config.jenkinsStash = (config.jenkinsStash ?: [:]) + [enable: true]
        }
        return config
    }
}
