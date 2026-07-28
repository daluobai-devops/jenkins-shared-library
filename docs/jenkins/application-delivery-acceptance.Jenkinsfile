@Library('jenkins-shared-library@test') _

import com.daluobai.jenkinslib.codeup.InMemoryCodeupRepositoryAdapter
import com.daluobai.jenkinslib.delivery.DeliveryRuntime
import com.daluobai.jenkinslib.delivery.InMemoryArtifactStore
import com.daluobai.jenkinslib.delivery.InMemoryDeliveryExecutionCoordinator
import com.daluobai.jenkinslib.delivery.InMemoryDeliveryStageAdapter
import com.daluobai.jenkinslib.delivery.InMemorySourceRepositoryAdapter

node('app-jgzly-app02') {
    stage('legacy-web') {
        deployWeb([
            SHARE_PARAM: [appName: 'acceptance-web'],
            DEPLOY_PIPELINE: [
                stepsBuildNpm: [enable: false],
                stepsStorage: [enable: false],
                stepsJavaWebDeployToWebServer: [enable: false]
            ]
        ])
    }

    stage('legacy-java') {
        deployJavaWeb([
            SHARE_PARAM: [appName: 'acceptance-java'],
            DEPLOY_PIPELINE: [
                stepsBuildMaven: [enable: false],
                stepsStorage: [enable: false],
                stepsDeploy: [enable: false]
            ]
        ])
    }

    stage('replacement-entry') {
        def source = new InMemorySourceRepositoryAdapter(
            ['https://example.invalid/app.git': [main: 'acceptance-sha']],
            ['https://example.invalid/app.git@acceptance-sha': ['app']]
        )
        def stages = new InMemoryDeliveryStageAdapter()
        def runtime = new DeliveryRuntime(
            source, stages, new InMemoryArtifactStore(), new InMemoryDeliveryExecutionCoordinator()
        )
        // Jenkinsfile 验收通过替代入口的显式 runtime seam；真实环境接入仍使用单参数 deliverApplication。
        def result = deliverApplication([
            primary: [DELIVERY: [
                application: [id: 'acceptance-current', type: 'JAVA'],
                targetEnvironment: 'test',
                operationMode: 'DEPLOY',
                source: [repository: 'https://example.invalid/app.git', reference: 'main', directory: 'app'],
                stages: [
                    build: [enabled: true, strategy: 'MAVEN', config: [:]],
                    storage: [enabled: false],
                    deploy: [enabled: false]
                ],
                notification: [enabled: false]
            ]],
            execution: [id: env.BUILD_TAG, number: env.BUILD_NUMBER]
        ], runtime)
        assert result.status == 'SUCCESS'
        assert result.resolvedSourceRevision == 'acceptance-sha'
        assert stages.events == ['initialize', 'build:JAVA:MAVEN:acceptance-sha', 'cleanup']
    }

    stage('dispatch-dry-run') {
        def declaration = '''
            def customConfig = [
                primary: [DELIVERY: [
                    application: [id: 'acceptance-dispatch', type: 'JAVA'],
                    targetEnvironment: 'test', operationMode: 'DEPLOY',
                    source: [repository: 'https://example.invalid/app.git', reference: 'main', directory: 'app'],
                    stages: [
                        build: [enabled: true, strategy: 'MAVEN', config: [:]],
                        storage: [enabled: false], deploy: [enabled: false]
                    ],
                    notification: [enabled: false]
                ]],
                execution: [id: 'dispatch-acceptance']
            ]
            deliverApplication(customConfig)
        '''
        def codeup = new InMemoryCodeupRepositoryAdapter(
            [[id: '1', name: 'acceptance-repo']],
            ['1': [[path: 'apps/Jenkinsfile.delivery']]],
            ['1:apps/Jenkinsfile.delivery': [status: 'FOUND', revision: 'config-sha', content: declaration]]
        )
        def runtime = new DeliveryRuntime(
            new InMemorySourceRepositoryAdapter(
                ['https://example.invalid/app.git': [main: 'acceptance-sha']],
                ['https://example.invalid/app.git@acceptance-sha': ['app']]
            ),
            new InMemoryDeliveryStageAdapter(),
            new InMemoryArtifactStore(),
            new InMemoryDeliveryExecutionCoordinator()
        )
        def summary = dispatchCodeupRepositories([
            token: 'acceptance-placeholder', organizationId: 'acceptance',
            allowedRepositoryNames: ['acceptance-repo'],
            jenkinsfileName: 'Jenkinsfile.delivery',
            allowedMethods: ['deliverApplication'], dryRun: true
        ], codeup, runtime)
        assert summary.preflightPassed == 1
        assert runtime.stages.events.isEmpty()
    }

    stage('rejected-preflight') {
        def runtime = new DeliveryRuntime(
            new InMemorySourceRepositoryAdapter(),
            new InMemoryDeliveryStageAdapter(),
            new InMemoryArtifactStore(),
            new InMemoryDeliveryExecutionCoordinator()
        )
        try {
            deliverApplication([
                primary: [DELIVERY: [
                    application: [id: 'rejected', type: 'JAVA'], targetEnvironment: 'test',
                    operationMode: 'TEARDOWN', stages: [
                        build: [enabled: false], storage: [enabled: false], deploy: [enabled: false]
                    ]
                ]],
                execution: [id: 'rejected']
            ], runtime)
            error('预检应当拒绝TEARDOWN')
        } catch (IllegalArgumentException expected) {
            assert runtime.stages.events.isEmpty()
        }
    }
}
