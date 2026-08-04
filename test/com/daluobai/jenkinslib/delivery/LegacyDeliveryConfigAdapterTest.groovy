package com.daluobai.jenkinslib.delivery

import com.daluobai.jenkinslib.vars.DeployPipelineVarTestSupport
import org.junit.jupiter.api.Test

import static org.junit.jupiter.api.Assertions.assertEquals

class LegacyDeliveryConfigAdapterTest extends DeployPipelineVarTestSupport {

    @Test
    void legacyReadinessProbesInheritSharedRetrySettings() {
        Map request = adapter().requestFor('JAVA', [
                DEPLOY_PIPELINE: [stepsDeploy: [readinessProbe: [
                        tcp             : [enable: true, port: 8000],
                        http            : [enable: true, port: 8000, path: '/actuator/health', timeout: 3],
                        cmd             : [enable: true, command: 'true', timeout: 4],
                        period          : 5,
                        failureThreshold: 20
                ]]]
        ])

        List<Map> readiness = request.primary.DELIVERY.stages.deploy.readiness as List<Map>
        assertEquals([
                [type: 'TCP', config: [period: 5, failureThreshold: 20, enable: true, port: 8000]],
                [type: 'HTTP', config: [period: 5, failureThreshold: 20, enable: true, port: 8000, path: '/actuator/health', timeout: 3]],
                [type: 'COMMAND', config: [period: 5, failureThreshold: 20, enable: true, command: 'true', timeout: 4]]
        ], readiness)
    }

    private static LegacyDeliveryConfigAdapter adapter() {
        Script steps = loadPipelineScript('vars/deployJavaWeb.groovy', [
                'config/config.json': disabledJavaConfig()
        ])
        return new LegacyDeliveryConfigAdapter(steps)
    }
}
