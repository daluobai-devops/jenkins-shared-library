package com.daluobai.jenkinslib.delivery

import com.daluobai.jenkinslib.constant.EFileReadType
import com.daluobai.jenkinslib.utils.ConfigMergeUtils
import com.daluobai.jenkinslib.utils.ConfigUtils
import com.daluobai.jenkinslib.utils.MapUtils
import com.daluobai.jenkinslib.utils.StrUtils

class LegacyDeliveryConfigAdapter implements Serializable {
    private final def steps

    LegacyDeliveryConfigAdapter(def steps) {
        this.steps = steps
    }

    Map requestFor(String applicationType, Map customConfig) {
        Map fullConfig = resolveLegacyConfig(applicationType, customConfig ?: [:])
        if (!(fullConfig.SHARE_PARAM instanceof Map)) {
            fullConfig.SHARE_PARAM = [:]
        }
        if (StrUtils.isBlank(fullConfig.SHARE_PARAM.appName)) {
            fullConfig.SHARE_PARAM.appName = steps.currentBuild.projectName
        }
        Map delivery = applicationType == 'JAVA' ? javaDelivery(fullConfig) : webDelivery(fullConfig)
        boolean noStages = !(delivery.stages.build.enabled || delivery.stages.storage.enabled || delivery.stages.deploy.enabled)
        Map environment = steps.binding?.hasVariable('env') ? steps.binding.getVariable('env') as Map : [:]
        return [
                primary            : [DELIVERY: delivery, DEFAULT_CONFIG: fullConfig.DEFAULT_CONFIG ?: [:]],
                execution          : [
                        id    : environment.BUILD_TAG ?: steps.currentBuild.fullDisplayName,
                        number: environment.BUILD_NUMBER ?: 1
                ],
                legacyCompatibility: true,
                legacyNoop         : noStages
        ]
    }

    Map resolveLegacyConfig(String applicationType, Map customConfig) {
        Map defaults = normalize(applicationType, new ConfigUtils(steps).readConfig(EFileReadType.RESOURCES, 'config/config.json'))
        Map extension = [:]
        String extensionPath = customConfig?.CONFIG_EXTEND?.configFullPath?.toString()
        if (StrUtils.isNotBlank(extensionPath)) {
            extension = normalize(applicationType, new ConfigUtils(steps).readOptionalConfigFromFullPath(extensionPath))
        }
        Map primary = normalize(applicationType, customConfig ?: [:])
        return MapUtils.deepCopy(ConfigMergeUtils.mergeParams(MapUtils.merge([defaults, extension, primary]), steps.params) as Map)
    }

    private static Map normalize(String applicationType, Map config) {
        Map normalized = MapUtils.deepCopy(config ?: [:])
        if (applicationType != 'JAVA' || !(normalized.DEPLOY_PIPELINE instanceof Map)) {
            return normalized
        }
        Map pipeline = normalized.DEPLOY_PIPELINE as Map
        if (!pipeline.containsKey('stepsBuildMaven')) {
            return normalized
        }
        Map legacy = MapUtils.deepCopy((pipeline.remove('stepsBuildMaven') ?: [:]) as Map)
        Map build = pipeline.stepsBuild instanceof Map ? MapUtils.deepCopy(pipeline.stepsBuild as Map) : [:]
        if (!build.containsKey('stepsBuildMaven')) {
            build.stepsBuildMaven = legacy
        }
        if (!build.containsKey('enable')) {
            build.enable = legacy.enable != null ? legacy.enable : true
        }
        pipeline.stepsBuild = build
        return normalized
    }

    private static Map javaDelivery(Map config) {
        Map build = (config.DEPLOY_PIPELINE?.stepsBuild ?: [:]) as Map
        Map storage = (config.DEPLOY_PIPELINE?.stepsStorage ?: [:]) as Map
        Map deploy = (config.DEPLOY_PIPELINE?.stepsDeploy ?: [:]) as Map
        Map strategy = (build.stepsBuildMaven ?: [:]) as Map
        return common(config, 'JAVA', [
                build  : [enabled: enabled(build), strategy: 'MAVEN', config: strategy],
                storage: [enabled: enabled(storage), target: [type: 'JENKINS_STASH'], config: storage],
                deploy : [
                        enabled  : enabled(deploy),
                        strategy : enabled(deploy.stepsTomcatDeploy as Map) ? 'TOMCAT' : 'JAVA_SERVICE',
                        nodes    : deploy.labels ?: [],
                        readiness: readiness(deploy.readinessProbe as Map),
                        config   : deploy
                ]
        ], strategy)
    }

    private static Map webDelivery(Map config) {
        Map build = (config.DEPLOY_PIPELINE?.stepsBuildNpm ?: [:]) as Map
        Map storage = (config.DEPLOY_PIPELINE?.stepsStorage ?: [:]) as Map
        Map deploy = (config.DEPLOY_PIPELINE?.stepsJavaWebDeployToWebServer ?: [:]) as Map
        Map artifactsByType = [
                TAR: [path: 'package/app.tar.gz', fileName: 'app.tar.gz'],
                ZIP: [path: 'package/app.zip', fileName: 'app.zip']
        ]
        Map artifact = artifactsByType[storage.archiveType?.toString()?.toUpperCase(Locale.ROOT)] ?: [:]
        return common(config, 'WEB', [
                build  : [enabled: enabled(build), strategy: 'NPM', artifact: artifact, config: build],
                storage: [enabled: enabled(storage), target: [type: 'JENKINS_STASH'], config: storage],
                deploy : [
                        enabled              : enabled(deploy),
                        strategy             : 'WEB_STATIC',
                        nodes                : deploy.labels ?: [],
                        directArtifactHandoff: false,
                        config               : deploy
                ]
        ], build)
    }

    private static Map common(Map config, String type, Map stages, Map sourceConfig) {
        return [
                application      : [id: config.SHARE_PARAM.appName, type: type],
                targetEnvironment: config.SHARE_PARAM.targetEnvironment ?: 'legacy',
                operationMode    : config.SHARE_PARAM.operationMode ?: 'DEPLOY',
                source           : [
                        repository   : sourceConfig.gitUrl ?: 'legacy://implicit',
                        reference    : sourceConfig.gitBranch ?: 'legacy',
                        directory    : sourceConfig.sourceDirectory ?: '.',
                        credentialsId: sourceConfig.credentialsId
                ],
                stages           : stages,
                notification     : [
                        enabled: config.SHARE_PARAM.message != null,
                        message: config.SHARE_PARAM.message
                ]
        ]
    }

    private static boolean enabled(Map config) {
        return config != null && !config.isEmpty() && config.enable != false
    }

    private static List<Map> readiness(Map probes) {
        if (!probes) {
            return []
        }
        Map sharedConfig = [:]
        if (probes.containsKey('period')) {
            sharedConfig.period = probes.period
        }
        if (probes.containsKey('failureThreshold')) {
            sharedConfig.failureThreshold = probes.failureThreshold
        }
        List<Map> result = []
        addReadiness(result, 'TCP', probes.tcp, sharedConfig)
        addReadiness(result, 'HTTP', probes.http, sharedConfig)
        addReadiness(result, 'COMMAND', probes.cmd, sharedConfig)
        return result
    }

    private static void addReadiness(List<Map> result, String type, Object rawProbe, Map sharedConfig) {
        if (!(rawProbe instanceof Map) || !enabled(rawProbe as Map)) {
            return
        }
        Map effectiveProbeConfig = MapUtils.deepCopy(sharedConfig)
        effectiveProbeConfig.putAll(MapUtils.deepCopy(rawProbe as Map))
        result.add([type: type, config: effectiveProbeConfig])
    }
}
