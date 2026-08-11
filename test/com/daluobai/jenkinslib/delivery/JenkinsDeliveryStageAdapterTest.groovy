package com.daluobai.jenkinslib.delivery

import com.daluobai.jenkinslib.steps.StepsBuildMaven
import com.daluobai.jenkinslib.steps.StepsBuildNpm
import org.junit.jupiter.api.Test

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertFalse
import static org.junit.jupiter.api.Assertions.assertTrue

class JenkinsDeliveryStageAdapterTest {
    @Test
    void unifiedJavaBuildConsumesPreparedSourceWorkingCopy() {
        List<String> calls = []
        StepsBuildMaven.metaClass.build = { Map ignored -> calls.add('legacy-build') }
        StepsBuildMaven.metaClass.buildFromSource = { Map config, String workingCopyRoot ->
            calls.add("prepared:${workingCopyRoot}:${config.DEPLOY_PIPELINE.stepsBuild.stepsBuildMaven.gitBranch}".toString())
        }
        try {
            new JenkinsDeliveryStageAdapter([:]).build([
                    DEFAULT_CONFIG: [docker: [registry: [domain: 'docker.io']]],
                    DELIVERY      : [
                            application: [id: 'orders', type: 'JAVA'],
                            source     : [repository: 'git@example.com:team/orders.git', reference: 'main', directory: 'services/orders'],
                            stages     : [
                                    build  : [enabled: true, strategy: 'MAVEN', artifact: [path: 'package/app.jar'], config: [lifecycle: 'package']],
                                    storage: [enabled: false],
                                    deploy : [enabled: false]
                            ],
                            notification: [enabled: false]
                    ]
            ], [resolvedSourceRevision: 'abc123'])

            assertEquals(['prepared:source:abc123'], calls)
            assertTrue(calls.every { !it.startsWith('legacy-build') })
        } finally {
            GroovySystem.metaClassRegistry.removeMetaClass(StepsBuildMaven)
        }
    }

    @Test
    void unifiedWebBuildConsumesPreparedSourceWorkingCopy() {
        List<String> calls = []
        StepsBuildNpm.metaClass.build = { Map ignored -> calls.add('legacy-build') }
        StepsBuildNpm.metaClass.buildFromSource = { Map config, String workingCopyRoot ->
            calls.add("prepared:${workingCopyRoot}:${config.DEPLOY_PIPELINE.stepsBuildNpm.gitBranch}".toString())
        }
        try {
            new JenkinsDeliveryStageAdapter([:]).build([
                    DEFAULT_CONFIG: [docker: [registry: [domain: 'docker.io']]],
                    DELIVERY      : [
                            application: [id: 'web', type: 'WEB'],
                            source     : [repository: 'git@example.com:team/web.git', reference: 'main', directory: 'frontend'],
                            stages     : [
                                    build  : [enabled: true, strategy: 'NPM', artifact: [path: 'package/app.zip'], config: [buildCMD: 'npm run build']],
                                    storage: [enabled: false],
                                    deploy : [enabled: false]
                            ],
                            notification: [enabled: false]
                    ]
            ], [resolvedSourceRevision: 'def456'])

            assertEquals(['prepared:source:def456'], calls)
            assertTrue(calls.every { !it.startsWith('legacy-build') })
        } finally {
            GroovySystem.metaClassRegistry.removeMetaClass(StepsBuildNpm)
        }
    }

    @Test
    void mapsNewDeployConfigDirectlyToJavaServiceStrategy() {
        Map config = [
                javaPath  : '/usr/local/jdk/jdk21/bin/java',
                pathRoot  : '/apps/application/',
                runOptions: '-Xms1024M',
                runArgs   : '--spring.profiles.active=prod'
        ]

        Map strategy = JenkinsDeliveryStageAdapter.strategyDeploymentConfig(
                config, 'stepsJavaWebDeployToService'
        )

        assertEquals(config, strategy)
    }

    @Test
    void preservesNestedLegacyJavaServiceConfig() {
        Map config = [
                labels                       : ['legacy-node'],
                stepsJavaWebDeployToService: [
                        pathRoot: '/legacy/apps',
                        javaPath: '/legacy/java'
                ],
                stepsTomcatDeploy            : [deployPath: '/legacy/tomcat']
        ]

        Map strategy = JenkinsDeliveryStageAdapter.strategyDeploymentConfig(
                config, 'stepsJavaWebDeployToService'
        )

        assertEquals('/legacy/apps', strategy.pathRoot)
        assertEquals('/legacy/java', strategy.javaPath)
        assertFalse(strategy.containsKey('labels'))
        assertFalse(strategy.containsKey('stepsTomcatDeploy'))
    }

    @Test
    void preservesNestedLegacyTomcatConfig() {
        Map config = [
                labels           : ['legacy-node'],
                stepsTomcatDeploy: [
                        tomcatHome: '/usr/local/tomcat',
                        deployPath: '/usr/local/tomcat/webapps'
                ]
        ]

        Map strategy = JenkinsDeliveryStageAdapter.strategyDeploymentConfig(
                config, 'stepsTomcatDeploy'
        )

        assertEquals('/usr/local/tomcat', strategy.tomcatHome)
        assertEquals('/usr/local/tomcat/webapps', strategy.deployPath)
        assertFalse(strategy.containsKey('labels'))
    }
}
