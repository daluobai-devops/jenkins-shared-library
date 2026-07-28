package com.daluobai.jenkinslib.vars

import com.daluobai.jenkinslib.steps.StepsBuildMaven
import com.daluobai.jenkinslib.steps.StepsBuildNpm
import com.daluobai.jenkinslib.utils.MessageUtils
import groovy.json.JsonOutput
import groovy.lang.GroovySystem
import org.jenkinsci.plugins.workflow.steps.FlowInterruptedException
import org.jenkinsci.plugins.workflow.steps.UserInterruption
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertFalse
import static org.junit.jupiter.api.Assertions.assertThrows
import static org.junit.jupiter.api.Assertions.assertTrue

class LegacyEntryCompatibilityBaselineTest extends DeployPipelineVarTestSupport {

    @AfterEach
    void cleanupMetaClasses() {
        GroovySystem.metaClassRegistry.removeMetaClass(MessageUtils)
        GroovySystem.metaClassRegistry.removeMetaClass(StepsBuildNpm)
        GroovySystem.metaClassRegistry.removeMetaClass(StepsBuildMaven)
    }

    @Test
    void deployWebPublicEntryPreservesConfigurationPrecedence() {
        assertEquals('发布开始：default-app', webStartTitle(
                [appName: 'default-app'],
                [:],
                [:],
                [:]
        ))
        assertEquals('发布开始：extend-app', webStartTitle(
                [appName: 'default-app'],
                [appName: 'extend-app'],
                [:],
                [:]
        ))
        assertEquals('发布开始：custom-app', webStartTitle(
                [appName: 'default-app'],
                [appName: 'extend-app'],
                [appName: 'custom-app'],
                [:]
        ))
        assertEquals('发布开始：parameter-app', webStartTitle(
                [appName: 'default-app'],
                [appName: 'extend-app'],
                [appName: 'custom-app'],
                ['SHARE_PARAM.appName': 'parameter-app']
        ))
    }

    @Test
    void deployJavaWebPublicEntryAcceptsLegacyBuildConfiguration() {
        Map defaultConfig = disabledJavaConfig([appName: 'default-app'])
        Script script = loadPipelineScript('vars/deployJavaWeb.groovy', [
                'config/config.json': defaultConfig,
                'config/extend.json': [
                        SHARE_PARAM    : [appName: 'extend-app'],
                        DEPLOY_PIPELINE: [
                                stepsBuildMaven: [enable: false, gitUrl: 'extend.git']
                        ]
                ]
        ])
        List<String> messageTitles = []
        int buildCalls = 0
        Map buildConfig = [:]
        stubMessageTitles(messageTitles)
        StepsBuildMaven.metaClass.build = { Map config ->
            buildCalls++
            buildConfig = config
        }

        script.invokeMethod('call', [[
                CONFIG_EXTEND : [configFullPath: 'RESOURCES:config/extend.json'],
                SHARE_PARAM   : [
                        appName: 'custom-app',
                        message: [wecom: [key: 'test']]
                ],
                DEPLOY_PIPELINE: [
                        stepsBuildMaven: [enable: true, gitUrl: 'custom.git']
                ]
        ]] as Object[])

        assertEquals(1, buildCalls)
        assertEquals('custom.git', buildConfig.DEPLOY_PIPELINE.stepsBuild.stepsBuildMaven.gitUrl)
        assertEquals(['发布开始：custom-app', '成功:custom-app'], messageTitles)
    }

    @Test
    void deployWebPublicEntrySkipsMissingExtensionButRejectsMalformedExtension() {
        Script missingScript = loadPipelineScript('vars/deployWeb.groovy', [
                'config/config.json': disabledWebConfig()
        ])
        List<String> messageTitles = []
        stubMessageTitles(messageTitles)

        missingScript.invokeMethod('call', [[
                CONFIG_EXTEND: [configFullPath: 'RESOURCES:config/missing.json'],
                SHARE_PARAM : [
                        appName: 'custom-app',
                        message: [wecom: [key: 'test']]
                ]
        ]] as Object[])

        assertEquals(['发布开始：custom-app', '成功:custom-app'], messageTitles)

        Script malformedScript = loadPipelineScript('vars/deployWeb.groovy', [
                'config/config.json': disabledWebConfig()
        ])
        malformedScript.metaClass.libraryResource = { String path ->
            return path == 'config/broken.json' ? '{' : JsonOutput.toJson(disabledWebConfig())
        }

        Exception failure = assertThrows(Exception.class) {
            malformedScript.invokeMethod('call', [[
                    CONFIG_EXTEND: [configFullPath: 'RESOURCES:config/broken.json']
            ]] as Object[])
        }

        assertFalse(failure.message?.contains('No such library resource'))
    }

    @Test
    void deployWebPublicEntryRethrowsNonMissingExtensionReadFailure() {
        Script script = loadPipelineScript('vars/deployWeb.groovy', [
                'config/config.json': disabledWebConfig()
        ])
        script.metaClass.libraryResource = { String path ->
            if (path == 'config/unavailable.json') {
                throw new IllegalStateException('resource store unavailable')
            }
            return JsonOutput.toJson(disabledWebConfig())
        }

        IllegalStateException failure = assertThrows(IllegalStateException.class) {
            script.invokeMethod('call', [[
                    CONFIG_EXTEND: [configFullPath: 'RESOURCES:config/unavailable.json']
            ]] as Object[])
        }

        assertEquals('resource store unavailable', failure.message)
    }

    @Test
    void deployWebPublicEntryPreservesInterruptionAndStillCleansWorkspace() {
        Script script = loadPipelineScript('vars/deployWeb.groovy', [
                'config/config.json': buildOnlyWebConfig()
        ])
        List<String> cleanupCalls = []
        List<String> messageTitles = []
        script.metaClass.deleteDir = { -> cleanupCalls.add('deleteDir') }
        StepsBuildNpm.metaClass.build = { Map ignored ->
            throw new FlowInterruptedException([new UserInterruption()])
        }
        stubMessageTitles(messageTitles)

        FlowInterruptedException failure = assertThrows(FlowInterruptedException.class) {
            script.invokeMethod('call', [[:]] as Object[])
        }

        assertEquals(1, failure.causes.size())
        assertTrue(failure.causes[0] instanceof UserInterruption)
        assertEquals(['deleteDir'], cleanupCalls)
        assertEquals(['发布开始：web-app'], messageTitles)
    }

    @Test
    void deployWebPublicEntryKeepsPrimaryFailureWhenFailureNotificationAlsoFails() {
        Script script = loadPipelineScript('vars/deployWeb.groovy', [
                'config/config.json': buildOnlyWebConfig()
        ])
        List<String> events = []
        script.metaClass.deleteDir = { -> events.add('cleanup') }
        StepsBuildNpm.metaClass.build = { Map ignored ->
            events.add('build-failed')
            throw new IllegalStateException('build failed')
        }
        MessageUtils.metaClass.sendMessage = { boolean ignoredSimpleMessage, Object ignoredConfig, String title, String ignoredContent ->
            events.add(title)
            return true
        }
        MessageUtils.metaClass.sendMessage = { Object ignoredConfig, String title, String ignoredContent ->
            events.add(title)
            throw new IllegalStateException('failure notification failed')
        }

        IllegalStateException failure = assertThrows(IllegalStateException.class) {
            script.invokeMethod('call', [[:]] as Object[])
        }

        assertEquals('build failed', failure.message)
        assertEquals([
                '发布开始：web-app',
                'build-failed',
                '失败:web-app',
                'cleanup'
        ], events)
    }

    @Test
    void deployWebPublicEntryFailsWhenCleanupFailsWithoutPrimaryFailure() {
        Script script = loadPipelineScript('vars/deployWeb.groovy', [
                'config/config.json': disabledWebConfig([appName: 'web-app'])
        ])
        script.metaClass.deleteDir = { ->
            throw new IllegalStateException('cleanup failed')
        }

        IllegalStateException failure = assertThrows(IllegalStateException.class) {
            script.invokeMethod('call', [[:]] as Object[])
        }

        assertEquals('cleanup failed', failure.message)
    }

    private static String webStartTitle(Map defaultShareParam,
                                        Map extensionShareParam,
                                        Map customShareParam,
                                        Map params) {
        Map<String, Map> resources = [
                'config/config.json': disabledWebConfig(defaultShareParam)
        ]
        Map customConfig = [:]
        if (!extensionShareParam.isEmpty()) {
            resources['config/extend.json'] = [SHARE_PARAM: extensionShareParam]
            customConfig.CONFIG_EXTEND = [configFullPath: 'RESOURCES:config/extend.json']
        }
        if (!customShareParam.isEmpty()) {
            customConfig.SHARE_PARAM = customShareParam
        }

        Script script = loadPipelineScript('vars/deployWeb.groovy', resources, params)
        List<String> messageTitles = []
        Map shareParam = (customConfig.SHARE_PARAM ?: [:]) as Map
        shareParam.message = [wecom: [key: 'test']]
        customConfig.SHARE_PARAM = shareParam
        stubMessageTitles(messageTitles)

        try {
            script.invokeMethod('call', [customConfig] as Object[])
            return messageTitles.first()
        } finally {
            GroovySystem.metaClassRegistry.removeMetaClass(MessageUtils)
        }
    }

    private static Map buildOnlyWebConfig() {
        return [
                SHARE_PARAM    : [
                        appName: 'web-app',
                        message: [wecom: [key: 'test']]
                ],
                DEPLOY_PIPELINE: [
                        stepsBuildNpm                : [enable: true],
                        stepsStorage                 : [enable: false],
                        stepsJavaWebDeployToWebServer: [enable: false]
                ]
        ]
    }

    private static void stubMessageTitles(List<String> messageTitles) {
        MessageUtils.metaClass.sendMessage = { boolean ignoredSimpleMessage, Object ignoredConfig, String title, String ignoredContent ->
            messageTitles.add(title)
            return true
        }
        MessageUtils.metaClass.sendMessage = { Object ignoredConfig, String title, String ignoredContent ->
            messageTitles.add(title)
            return true
        }
    }
}
