package com.daluobai.jenkinslib.vars

import com.daluobai.jenkinslib.codeup.InMemoryCodeupRepositoryAdapter
import com.daluobai.jenkinslib.delivery.DeliveryRuntime
import com.daluobai.jenkinslib.delivery.InMemoryArtifactStore
import com.daluobai.jenkinslib.delivery.InMemoryDeliveryExecutionCoordinator
import com.daluobai.jenkinslib.delivery.InMemoryDeliveryStageAdapter
import com.daluobai.jenkinslib.delivery.InMemorySourceRepositoryAdapter
import groovy.lang.Binding
import groovy.lang.GroovyShell
import org.junit.jupiter.api.Test

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertThrows
import static org.junit.jupiter.api.Assertions.assertTrue

class RepositoryDispatchEntryTest {

    @Test
    void dryRunRecursivelyPreflightsAllUnitsInDeterministicOrderWithoutDeliverySideEffects() {
        InMemoryCodeupRepositoryAdapter codeup = new InMemoryCodeupRepositoryAdapter([
                [id: '2', name: 'repo-b'],
                [id: '1', name: 'repo-a']
        ], [
                '1': [[path: 'z/Jenkinsfile.delivery'], [path: 'a/Jenkinsfile.delivery']],
                '2': [[path: 'Jenkinsfile.delivery']]
        ], [
                '1:a/Jenkinsfile.delivery': found('r1a', declaration('app-a', 'services/app')),
                '1:z/Jenkinsfile.delivery': found('r1z', declaration('app-z', 'services/app')),
                '2:Jenkinsfile.delivery'  : found('r2', declaration('app-b', 'services/app'))
        ])
        InMemoryDeliveryStageAdapter stages = new InMemoryDeliveryStageAdapter()
        Script script = loadEntry(codeup, runtime(stages))

        Map result = script.invokeMethod('call', [[
                token                 : 'token',
                organizationId        : 'org',
                allowedRepositoryNames: ['repo-a', 'repo-b'],
                jenkinsfileName       : 'Jenkinsfile.delivery',
                allowedMethods        : ['deliverApplication'],
                dryRun                : true
        ]] as Object[]) as Map

        assertEquals(['repo-a:a/Jenkinsfile.delivery', 'repo-a:z/Jenkinsfile.delivery', 'repo-b:Jenkinsfile.delivery'], result.units*.unitId)
        assertEquals(3, result.preflightPassed, result.toString())
        assertEquals(0, result.dispatched)
        assertTrue(stages.events.isEmpty())
        assertEquals(3, codeup.readCounts.values().sum())
    }

    @Test
    void dispatchDistinguishesWithdrawnRejectedAndReadFailureThenFailsAtEndAfterSuccess() {
        InMemoryCodeupRepositoryAdapter codeup = new InMemoryCodeupRepositoryAdapter([
                [id: '1', name: 'repo-a']
        ], [
                '1': [
                        [path: '01/Jenkinsfile.delivery'],
                        [path: '02/Jenkinsfile.delivery'],
                        [path: '03/Jenkinsfile.delivery'],
                        [path: '04/Jenkinsfile.delivery']
                ]
        ], [
                '1:01/Jenkinsfile.delivery': found('c1', declaration('app-ok', 'services/app')),
                '1:02/Jenkinsfile.delivery': [status: 'WITHDRAWN', revision: 'c2'],
                '1:03/Jenkinsfile.delivery': [status: 'FAILED', reason: 'transport failed'],
                '1:04/Jenkinsfile.delivery': found('c4', 'sh "rm -rf /"')
        ])
        InMemoryDeliveryStageAdapter stages = new InMemoryDeliveryStageAdapter()
        Script script = loadEntry(codeup, runtime(stages))

        IllegalStateException failure = assertThrows(IllegalStateException.class) {
            script.invokeMethod('call', [[
                    token                 : 'token',
                    organizationId        : 'org',
                    allowedRepositoryNames: ['repo-a'],
                    jenkinsfileName       : 'Jenkinsfile.delivery',
                    allowedMethods        : ['deliverApplication'],
                    failAtEnd             : true
            ]] as Object[])
        }

        assertTrue(failure.message.contains('失败: 1'))
        assertTrue(failure.message.contains('拒绝: 1'), failure.message)
        assertEquals(['initialize', 'build:JAVA:MAVEN:abc123', 'cleanup'], stages.events)
        assertEquals(1, codeup.readCounts['1:01/Jenkinsfile.delivery'])
    }

    @Test
    void dispatchRecordsSkippedRepositoryAndRejectsDuplicateApplicationIdentifier() {
        InMemoryCodeupRepositoryAdapter codeup = new InMemoryCodeupRepositoryAdapter([
                [id: '1', name: 'repo-a'], [id: '2', name: 'repo-b']
        ], [
                '1': [[path: 'a/Jenkinsfile.delivery'], [path: 'b/Jenkinsfile.delivery']],
                '2': [[path: 'Jenkinsfile.delivery']]
        ], [
                '1:a/Jenkinsfile.delivery': found('a1', declaration('duplicate', 'services/app')),
                '1:b/Jenkinsfile.delivery': found('a2', declaration('duplicate', 'services/app'))
        ])
        Script script = loadEntry(codeup, runtime(new InMemoryDeliveryStageAdapter()))

        Map result = script.invokeMethod('call', [[
                token                 : 'token', organizationId: 'org',
                allowedRepositoryNames: ['repo-a'], jenkinsfileName: 'Jenkinsfile.delivery',
                allowedMethods        : ['deliverApplication'], dryRun: true
        ]] as Object[]) as Map

        assertEquals(0, result.preflightPassed)
        assertEquals(2, result.rejected.size())
        assertTrue(result.rejected.every { it.reason.contains('应用标识冲突') })
        assertEquals(1, result.skipped)
        assertEquals('SKIPPED', result.units.find { it.repositoryName == 'repo-b' }.status)
    }

    private static Script loadEntry(InMemoryCodeupRepositoryAdapter codeup, DeliveryRuntime runtime) {
        Binding binding = new Binding([codeupRepositoryAdapter: codeup, deliveryRuntime: runtime])
        Script script = new GroovyShell(DeliveryRuntime.class.classLoader, binding)
                .parse(new File('vars/dispatchCodeupRepositories.groovy'))
        script.metaClass.echo = { Object ignored -> }
        script.metaClass.error = { Object message -> throw new IllegalStateException(message.toString()) }
        return script
    }

    private static DeliveryRuntime runtime(InMemoryDeliveryStageAdapter stages) {
        return new DeliveryRuntime(
                new InMemorySourceRepositoryAdapter([
                        'git@example/app.git': [main: 'abc123']
                ], [
                        'git@example/app.git@abc123': ['services/app']
                ]),
                stages,
                new InMemoryArtifactStore(),
                new InMemoryDeliveryExecutionCoordinator()
        )
    }

    private static Map found(String revision, String content) {
        return [status: 'FOUND', revision: revision, content: content]
    }

    private static String declaration(String applicationId, String directory) {
        return """
                def customConfig = [
                    primary: [DELIVERY: [
                        application: [id: '${applicationId}', type: 'JAVA'],
                        targetEnvironment: 'test',
                        operationMode: 'DEPLOY',
                        source: [repository: 'git@example/app.git', reference: 'main', directory: '${directory}'],
                        stages: [
                            build: [enabled: true, strategy: 'MAVEN', config: [:]],
                            storage: [enabled: false],
                            deploy: [enabled: false]
                        ],
                        notification: [enabled: false]
                    ]],
                    execution: [id: '${applicationId}-execution']
                ]
                deliverApplication(customConfig)
                """.stripIndent()
    }
}
