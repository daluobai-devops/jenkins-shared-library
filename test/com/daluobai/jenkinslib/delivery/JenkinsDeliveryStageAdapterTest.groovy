package com.daluobai.jenkinslib.delivery

import org.junit.jupiter.api.Test

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertFalse

class JenkinsDeliveryStageAdapterTest {
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
