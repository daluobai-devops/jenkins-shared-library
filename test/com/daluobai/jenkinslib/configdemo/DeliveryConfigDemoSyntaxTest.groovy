package com.daluobai.jenkinslib.configdemo

import groovy.lang.GroovyShell
import org.junit.jupiter.api.Test

import static org.junit.jupiter.api.Assertions.assertFalse
import static org.junit.jupiter.api.Assertions.assertTrue

class DeliveryConfigDemoSyntaxTest {
    @Test
    void newDeliveryDemosUseCurrentConfigurationAndCompile() {
        [
                new File('configdemo/deliverApplicationWeb.groovy'),
                new File('configdemo/deliverApplicationJavaWeb.groovy')
        ].each { File demo ->
            assertTrue(demo.isFile(), "示例文件不存在: ${demo}")
            String source = demo.getText('UTF-8')
            assertTrue(source.contains('deliverApplication(deliveryConfig)'))
            assertTrue(source.contains('DELIVERY: ['))
            String executableSource = source.readLines()
                    .findAll { String line -> !line.trim().startsWith('//') }
                    .join('\n')
            assertFalse(executableSource.contains('DEPLOY_PIPELINE'))
            assertFalse(executableSource.contains("node('buildNode')"))

            String compilableSource = source.replaceFirst(
                    /@Library\('jenkins-shared-library'\) _/,
                    ''
            )
            new GroovyShell().parse(compilableSource)
        }
    }
}
