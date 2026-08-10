package com.daluobai.jenkinslib.delivery

import com.daluobai.jenkinslib.vars.DeployPipelineVarTestSupport
import org.junit.jupiter.api.Test

import static org.junit.jupiter.api.Assertions.assertEquals

class LegacyDeliveryConfigAdapterTest extends DeployPipelineVarTestSupport {

    @Test
    void legacyWebArchiveTypesPopulateUnifiedArtifactBeforePreflight() {
        Script steps = loadPipelineScript('vars/deployWeb.groovy', [
                'config/config.json': disabledWebConfig()
        ])
        LegacyDeliveryConfigAdapter adapter = new LegacyDeliveryConfigAdapter(steps)

        [
                [archiveType: 'TAR', artifact: [path: 'package/app.tar.gz', fileName: 'app.tar.gz']],
                [archiveType: 'ZIP', artifact: [path: 'package/app.zip', fileName: 'app.zip']]
        ].each { Map scenario ->
            Map request = adapter.requestFor('WEB', [
                    SHARE_PARAM    : [appName: 'web-app'],
                    DEPLOY_PIPELINE: [
                            stepsBuildNpm                : [
                                    enable   : true,
                                    gitUrl   : 'git@example.com:web-app.git',
                                    gitBranch: 'main',
                                    buildCMD : 'npm ci && npm run build'
                            ],
                            stepsStorage                 : [
                                    enable     : true,
                                    archiveType: scenario.archiveType
                            ],
                            stepsJavaWebDeployToWebServer: [
                                    enable  : true,
                                    pathRoot: '/srv/web',
                                    labels  : ['web-node']
                            ]
                    ]
            ])

            assertEquals(scenario.artifact, request.primary.DELIVERY.stages.build.artifact)
            Map preflight = new DeliveryPreflight(
                    new LegacySourceRepositoryAdapter(),
                    new LegacyJenkinsDeliveryStageAdapter(steps)
            ).validate(request.primary)
            assertEquals([build: true, storage: true, deploy: true], preflight.stages)
        }
    }

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
