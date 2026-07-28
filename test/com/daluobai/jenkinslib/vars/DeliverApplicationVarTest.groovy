package com.daluobai.jenkinslib.vars

import com.daluobai.jenkinslib.delivery.DeliveryRuntime
import com.daluobai.jenkinslib.delivery.DeliveryExecutionException
import com.daluobai.jenkinslib.delivery.InMemoryArtifactStore
import com.daluobai.jenkinslib.delivery.InMemoryDeliveryExecutionCoordinator
import com.daluobai.jenkinslib.delivery.InMemoryDeliveryStageAdapter
import com.daluobai.jenkinslib.delivery.InMemorySourceRepositoryAdapter
import groovy.lang.Binding
import groovy.lang.GroovyShell
import org.junit.jupiter.api.Test
import org.jenkinsci.plugins.workflow.steps.FlowInterruptedException

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertFalse
import static org.junit.jupiter.api.Assertions.assertThrows
import static org.junit.jupiter.api.Assertions.assertTrue

class DeliverApplicationVarTest {

    @Test
    void replacementEntryAllocatesDefaultBuildNodeInternally() {
        List<String> allocatedNodes = []
        Script script = loadEntry(runtimeFor(new InMemoryDeliveryStageAdapter()))
        script.metaClass.node = { String label, Closure body ->
            allocatedNodes.add(label)
            body.call()
        }

        Map delivery = baseJavaDelivery()
        delivery.source.reference = 'main'
        Map result = script.invokeMethod('call', [[
                primary  : [DELIVERY: delivery],
                execution: [id: 'default-build-node']
        ]] as Object[]) as Map

        assertEquals('SUCCESS', result.status)
        assertEquals(['buildNode'], allocatedNodes)
    }

    @Test
    void replacementEntryBuildsJavaFromPinnedRevisionAndReturnsTraceableResult() {
        InMemorySourceRepositoryAdapter source = new InMemorySourceRepositoryAdapter([
                'git@example/app.git': [main: 'abc123']
        ], [
                'git@example/app.git@abc123': ['services/app']
        ])
        InMemoryDeliveryStageAdapter stages = new InMemoryDeliveryStageAdapter()
        DeliveryRuntime runtime = new DeliveryRuntime(
                source,
                stages,
                new InMemoryArtifactStore(),
                new InMemoryDeliveryExecutionCoordinator()
        )
        Script script = loadEntry(runtime)

        Map result = script.invokeMethod('call', [[
                defaults : [DELIVERY: baseJavaDelivery()],
                extension: [DELIVERY: [targetEnvironment: 'test']],
                primary  : [DELIVERY: [application: [id: 'orders']]],
                overrides: [DELIVERY: [source: [reference: 'main']]],
                execution: [id: 'job-42']
        ]] as Object[]) as Map

        assertEquals('SUCCESS', result.status)
        assertEquals('abc123', result.resolvedSourceRevision)
        assertEquals('main', result.sourceReference)
        assertEquals('orders', result.applicationId)
        assertEquals('test', result.targetEnvironment)
        assertEquals(['defaults', 'extension', 'primary', 'overrides'], result.configurationSources)
        assertEquals(['initialize', 'build:JAVA:MAVEN:abc123', 'cleanup'], stages.events)
        assertEquals(['abc123'], source.checkedOutRevisions)
    }

    @Test
    void replacementEntryRejectsLegacyLayoutBeforeDeliverySideEffects() {
        InMemoryDeliveryStageAdapter stages = new InMemoryDeliveryStageAdapter()
        Script script = loadEntry(runtimeFor(stages))

        IllegalArgumentException failure = assertThrows(IllegalArgumentException.class) {
            script.invokeMethod('call', [[
                    primary  : [DEPLOY_PIPELINE: [stepsBuildMaven: [enable: true]]],
                    execution: [id: 'job-legacy']
            ]] as Object[])
        }

        assertTrue(failure.message.contains('不接受旧配置结构'))
        assertTrue(stages.events.isEmpty())
    }

    @Test
    void replacementEntryRejectsInvalidStageCombinationWithoutSideEffects() {
        InMemoryDeliveryStageAdapter stages = new InMemoryDeliveryStageAdapter()
        Script script = loadEntry(runtimeFor(stages))
        Map invalid = baseJavaDelivery()
        invalid.stages.build.enabled = false
        invalid.stages.storage = [enabled: true, target: [type: 'JENKINS']]

        IllegalArgumentException failure = assertThrows(IllegalArgumentException.class) {
            script.invokeMethod('call', [[primary: [DELIVERY: invalid], execution: [id: 'job-invalid']]] as Object[])
        }

        assertTrue(failure.message.contains('要求启用构建阶段'))
        assertTrue(stages.events.isEmpty())
    }

    @Test
    void replacementEntryKeepsResolvedRevisionWhenReferenceMovesBeforeBuild() {
        InMemorySourceRepositoryAdapter source = sourceRepository()
        InMemoryDeliveryStageAdapter stages = new InMemoryDeliveryStageAdapter()
        stages.beforeBuild = {
            source.revisions['git@example/app.git'].main = 'moved999'
        }
        Script script = loadEntry(new DeliveryRuntime(
                source, stages, new InMemoryArtifactStore(), new InMemoryDeliveryExecutionCoordinator()
        ))
        Map delivery = baseJavaDelivery()
        delivery.source.reference = 'main'

        Map result = script.invokeMethod('call', [[primary: [DELIVERY: delivery], execution: [id: 'job-pin']]] as Object[]) as Map

        assertEquals('abc123', result.resolvedSourceRevision)
        assertEquals(['abc123'], source.checkedOutRevisions)
    }

    @Test
    void replacementEntryStoresArtifactWithImmutableNamespacedIdentity() {
        InMemoryDeliveryStageAdapter stages = new InMemoryDeliveryStageAdapter()
        InMemoryArtifactStore store = new InMemoryArtifactStore()
        Script script = loadEntry(new DeliveryRuntime(
                sourceRepository(), stages, store, new InMemoryDeliveryExecutionCoordinator()
        ))
        Map delivery = baseJavaDelivery()
        delivery.source.reference = 'main'
        delivery.stages.storage = [enabled: true, target: [type: 'JENKINS']]

        Map result = script.invokeMethod('call', [[primary: [DELIVERY: delivery], execution: [id: 'job-42']]] as Object[]) as Map

        assertEquals('default/default-app/job-42', result.artifact.identity)
        assertEquals('sha256:test', result.artifact.checksum)
        assertEquals('memory://default/default-app/job-42', result.artifact.reference)
        assertEquals(['initialize', 'build:JAVA:MAVEN:abc123', 'store', 'cleanup'], stages.events)
        assertTrue(store.artifacts.containsKey('default/default-app/job-42'))

        DeliveryExecutionException duplicate = assertThrows(DeliveryExecutionException.class) {
            script.invokeMethod('call', [[primary: [DELIVERY: delivery], execution: [id: 'job-42']]] as Object[])
        }
        assertEquals('FAILED', duplicate.deliveryResult.status)
        assertTrue(duplicate.message.contains('已存在'))
    }

    @Test
    void replacementEntryDoesNotStoreWhenBuildFailsAndExposesFailureResult() {
        InMemoryDeliveryStageAdapter stages = new InMemoryDeliveryStageAdapter()
        stages.buildFailure = new IllegalStateException('maven failed')
        InMemoryArtifactStore store = new InMemoryArtifactStore()
        Script script = loadEntry(new DeliveryRuntime(
                sourceRepository(), stages, store, new InMemoryDeliveryExecutionCoordinator()
        ))
        Map delivery = baseJavaDelivery()
        delivery.source.reference = 'main'
        delivery.stages.storage = [enabled: true, target: [type: 'JENKINS']]

        DeliveryExecutionException failure = assertThrows(DeliveryExecutionException.class) {
            script.invokeMethod('call', [[primary: [DELIVERY: delivery], execution: [id: 'job-build-fail']]] as Object[])
        }

        assertEquals('FAILED', failure.deliveryResult.status)
        assertEquals('maven failed', failure.deliveryResult.failure.message)
        assertTrue(store.artifacts.isEmpty())
        assertFalse(stages.events.contains('store'))
        assertEquals('cleanup', stages.events.last())
    }

    @Test
    void replacementEntryDeliversWebInBuildStoreDeployOrder() {
        InMemoryDeliveryStageAdapter stages = new InMemoryDeliveryStageAdapter()
        Script script = loadEntry(runtimeFor(stages))
        Map delivery = baseWebDelivery()

        Map result = script.invokeMethod('call', [[primary: [DELIVERY: delivery], execution: [id: 'web-1']]] as Object[]) as Map

        assertEquals('SUCCESS', result.status)
        assertEquals([
                'initialize',
                'notify:STARTED',
                'build:WEB:NPM:abc123',
                'store',
                'deploy:web-a',
                'notify:SUCCESS',
                'cleanup'
        ], stages.events)
    }

    @Test
    void replacementEntryStopsJavaSerialDeploymentAndReadinessAtFirstFailure() {
        InMemoryDeliveryStageAdapter stages = new InMemoryDeliveryStageAdapter()
        stages.deploymentFailureNode = 'node-b'
        Script script = loadEntry(runtimeFor(stages))
        Map delivery = baseJavaDelivery()
        delivery.source.reference = 'main'
        delivery.notification.enabled = true
        delivery.stages.storage = [enabled: true, target: [type: 'JENKINS_STASH'], config: [archiveType: 'JAR']]
        delivery.stages.deploy = [
                enabled   : true,
                strategy  : 'JAVA_SERVICE',
                nodes     : ['node-a', 'node-b', 'node-c'],
                readiness : [[type: 'TCP'], [type: 'HTTP'], [type: 'COMMAND']],
                config    : [:]
        ]

        DeliveryExecutionException failure = assertThrows(DeliveryExecutionException.class) {
            script.invokeMethod('call', [[primary: [DELIVERY: delivery], execution: [id: 'java-deploy']]] as Object[])
        }

        assertEquals('FAILED', failure.deliveryResult.status)
        assertTrue(stages.events.containsAll(['deploy:node-a', 'ready:node-a:TCP', 'ready:node-a:HTTP', 'ready:node-a:COMMAND', 'deploy:node-b']))
        assertFalse(stages.events.any { it.contains('node-c') })
        assertFalse(stages.events.any { it.startsWith('ready:node-b') })
        assertEquals('cleanup', stages.events.last())
    }

    @Test
    void replacementEntryKeepsPrimaryFailureWhenNotificationAndCleanupAlsoFail() {
        InMemoryDeliveryStageAdapter stages = new InMemoryDeliveryStageAdapter()
        stages.buildFailure = new IllegalStateException('primary build failure')
        stages.notificationFailures.FAILED = new IllegalStateException('notification failure')
        stages.cleanupFailure = new IllegalStateException('cleanup failure')
        Map delivery = baseJavaDelivery()
        delivery.source.reference = 'main'
        delivery.notification.enabled = true
        Script script = loadEntry(runtimeFor(stages))

        DeliveryExecutionException failure = assertThrows(DeliveryExecutionException.class) {
            script.invokeMethod('call', [[primary: [DELIVERY: delivery], execution: [id: 'failure-order']]] as Object[])
        }

        assertEquals('primary build failure', failure.message)
        assertEquals(['notification failure', 'cleanup failure'], failure.deliveryResult.warnings*.message)
        assertEquals('cleanup', stages.events.last())
    }

    @Test
    void replacementEntryFailsSuccessfulDeliveryWhenCleanupFails() {
        InMemoryDeliveryStageAdapter stages = new InMemoryDeliveryStageAdapter()
        stages.cleanupFailure = new IllegalStateException('cleanup failure')
        Map delivery = baseJavaDelivery()
        delivery.source.reference = 'main'
        Script script = loadEntry(runtimeFor(stages))

        DeliveryExecutionException failure = assertThrows(DeliveryExecutionException.class) {
            script.invokeMethod('call', [[primary: [DELIVERY: delivery], execution: [id: 'cleanup-fail']]] as Object[])
        }

        assertEquals('cleanup failure', failure.message)
        assertEquals('FAILED', failure.deliveryResult.status)
    }

    @Test
    void replacementEntryPreservesJenkinsInterruptionAndStillCleansWorkspace() {
        InMemoryDeliveryStageAdapter stages = new InMemoryDeliveryStageAdapter()
        stages.buildFailure = new FlowInterruptedException([])
        Map delivery = baseJavaDelivery()
        delivery.source.reference = 'main'

        assertThrows(FlowInterruptedException.class) {
            loadEntry(runtimeFor(stages)).invokeMethod('call', [[
                    primary: [DELIVERY: delivery], execution: [id: 'replacement-abort']
            ]] as Object[])
        }

        assertEquals('cleanup', stages.events.last())
    }

    @Test
    void replacementEntrySupersedesActiveExecutionBeforeWorkspaceInitialization() {
        InMemoryDeliveryStageAdapter stages = new InMemoryDeliveryStageAdapter()
        InMemoryDeliveryExecutionCoordinator coordinator = new InMemoryDeliveryExecutionCoordinator([
                id: 'old-7', applicationId: 'default-app', targetEnvironment: 'default', deploymentStarted: true
        ])
        Script script = loadEntry(new DeliveryRuntime(
                sourceRepository(), stages, new InMemoryArtifactStore(), coordinator
        ))
        Map delivery = baseJavaDelivery()
        delivery.source.reference = 'main'

        script.invokeMethod('call', [[primary: [DELIVERY: delivery], execution: [id: 'new-8']]] as Object[])

        assertEquals([[oldExecution: 'old-7', newExecution: 'new-8', applicationId: 'default-app', targetEnvironment: 'default', deploymentStarted: true]], coordinator.supersessions)
        assertEquals('before:new-8', coordinator.events.first())
        assertEquals('initialize', stages.events.first())
    }

    @Test
    void replacementEntryRejectsEveryUnsupportedPreflightCombinationWithoutNotifications() {
        List<Map> invalidDeliveries = []
        Map empty = baseJavaDelivery()
        empty.notification.enabled = true
        empty.stages.build.enabled = false
        invalidDeliveries.add(empty)

        Map unsupportedMode = baseJavaDelivery()
        unsupportedMode.notification.enabled = true
        unsupportedMode.operationMode = 'TEARDOWN'
        invalidDeliveries.add(unsupportedMode)

        Map multipleStorage = baseJavaDelivery()
        multipleStorage.notification.enabled = true
        multipleStorage.stages.storage = [enabled: true, targets: [[type: 'JENKINS'], [type: 'S3']]]
        invalidDeliveries.add(multipleStorage)

        Map multipleDeploy = baseJavaDelivery()
        multipleDeploy.notification.enabled = true
        multipleDeploy.stages.deploy = [enabled: true, strategies: ['JAVA_SERVICE', 'TOMCAT'], nodes: ['node-a']]
        invalidDeliveries.add(multipleDeploy)

        Map unreachable = baseJavaDelivery()
        unreachable.notification.enabled = true
        unreachable.stages.deploy = [enabled: true, strategy: 'JAVA_SERVICE', nodes: ['node-a'], directArtifactHandoff: false]
        invalidDeliveries.add(unreachable)

        Map unsupportedBuild = baseJavaDelivery()
        unsupportedBuild.stages.build.strategy = 'GRADLE'
        invalidDeliveries.add(unsupportedBuild)

        Map escapingDirectory = baseJavaDelivery()
        escapingDirectory.source.directory = '../other-app'
        invalidDeliveries.add(escapingDirectory)

        invalidDeliveries.eachWithIndex { Map delivery, int index ->
            InMemoryDeliveryStageAdapter stages = new InMemoryDeliveryStageAdapter()
            Script script = loadEntry(runtimeFor(stages))
            assertThrows(IllegalArgumentException.class) {
                script.invokeMethod('call', [[primary: [DELIVERY: delivery], execution: [id: "invalid-${index}"]]] as Object[])
            }
            assertTrue(stages.events.isEmpty(), "invalid preflight ${index} produced delivery side effects")
        }

        InMemoryDeliveryStageAdapter unavailableStages = new InMemoryDeliveryStageAdapter()
        unavailableStages.unavailableNodes.add('node-missing')
        Map unavailable = baseJavaDelivery()
        unavailable.notification.enabled = true
        unavailable.stages.storage = [enabled: true, target: [type: 'JENKINS']]
        unavailable.stages.deploy = [enabled: true, strategy: 'JAVA_SERVICE', nodes: ['node-missing']]
        assertThrows(IllegalArgumentException.class) {
            loadEntry(runtimeFor(unavailableStages)).invokeMethod('call', [[
                    primary: [DELIVERY: unavailable], execution: [id: 'unavailable-node']
            ]] as Object[])
        }
        assertTrue(unavailableStages.events.isEmpty())
    }

    @Test
    void replacementEntryAllowsBuildAndDeployWithExplicitDirectArtifactHandoff() {
        InMemoryDeliveryStageAdapter stages = new InMemoryDeliveryStageAdapter()
        Map delivery = baseJavaDelivery()
        delivery.source.reference = 'main'
        delivery.stages.deploy = [
                enabled: true, strategy: 'JAVA_SERVICE', nodes: ['node-a'],
                directArtifactHandoff: true, readiness: [], config: [:]
        ]

        Map result = loadEntry(runtimeFor(stages)).invokeMethod('call', [[
                primary: [DELIVERY: delivery], execution: [id: 'direct-handoff']
        ]] as Object[]) as Map

        assertEquals('SUCCESS', result.status)
        assertEquals(['initialize', 'build:JAVA:MAVEN:abc123', 'deploy:node-a', 'cleanup'], stages.events)
    }

    @Test
    void replacementEntryResolvesBranchTagAndCommitReferencesBeforeExecution() {
        InMemorySourceRepositoryAdapter source = new InMemorySourceRepositoryAdapter([
                'git@example/app.git': [main: 'branch-sha', 'v1.2.0': 'tag-sha', 'deadbeef': 'deadbeef']
        ], [
                'git@example/app.git@branch-sha': ['services/app'],
                'git@example/app.git@tag-sha'   : ['services/app'],
                'git@example/app.git@deadbeef'  : ['services/app']
        ])
        ['main': 'branch-sha', 'v1.2.0': 'tag-sha', 'deadbeef': 'deadbeef'].each { String reference, String revision ->
            Map delivery = baseJavaDelivery()
            delivery.source.reference = reference
            Map result = loadEntry(new DeliveryRuntime(
                    source, new InMemoryDeliveryStageAdapter(), new InMemoryArtifactStore(), new InMemoryDeliveryExecutionCoordinator()
            )).invokeMethod('call', [[primary: [DELIVERY: delivery], execution: [id: reference]]] as Object[]) as Map
            assertEquals(revision, result.resolvedSourceRevision)
        }
    }

    @Test
    void replacementEntryNamespacesSameNamedArtifactsAndReportsStorageFailure() {
        InMemoryArtifactStore store = new InMemoryArtifactStore()
        ['app-a', 'app-b'].each { String applicationId ->
            Map delivery = baseJavaDelivery()
            delivery.application.id = applicationId
            delivery.source.reference = 'main'
            delivery.stages.storage = [enabled: true, target: [type: 'JENKINS']]
            loadEntry(new DeliveryRuntime(
                    sourceRepository(), new InMemoryDeliveryStageAdapter(), store, new InMemoryDeliveryExecutionCoordinator()
            )).invokeMethod('call', [[primary: [DELIVERY: delivery], execution: [id: 'same-build']]] as Object[])
        }
        assertEquals(['default/app-a/same-build', 'default/app-b/same-build'] as Set, store.artifacts.keySet() as Set)

        InMemoryArtifactStore failingStore = new InMemoryArtifactStore()
        failingStore.failure = new IllegalStateException('storage unavailable')
        Map delivery = baseJavaDelivery()
        delivery.source.reference = 'main'
        delivery.stages.storage = [enabled: true, target: [type: 'JENKINS']]
        DeliveryExecutionException failure = assertThrows(DeliveryExecutionException.class) {
            loadEntry(new DeliveryRuntime(
                    sourceRepository(), new InMemoryDeliveryStageAdapter(), failingStore, new InMemoryDeliveryExecutionCoordinator()
            )).invokeMethod('call', [[primary: [DELIVERY: delivery], execution: [id: 'store-fail']]] as Object[])
        }
        assertEquals('storage unavailable', failure.message)
        assertEquals('FAILED', failure.deliveryResult.status)
    }

    private static Script loadEntry(DeliveryRuntime runtime) {
        Binding binding = new Binding([
                deliveryRuntime: runtime,
                currentBuild   : [number: 42, fullDisplayName: 'delivery #42']
        ])
        Script script = new GroovyShell(DeliveryRuntime.class.classLoader, binding)
                .parse(new File('vars/deliverApplication.groovy'))
        script.metaClass.echo = { Object ignored -> }
        script.metaClass.node = { String ignored, Closure body -> body.call() }
        return script
    }

    private static DeliveryRuntime runtimeFor(InMemoryDeliveryStageAdapter stages) {
        return new DeliveryRuntime(
                sourceRepository(), stages, new InMemoryArtifactStore(), new InMemoryDeliveryExecutionCoordinator()
        )
    }

    private static InMemorySourceRepositoryAdapter sourceRepository() {
        return new InMemorySourceRepositoryAdapter([
                'git@example/app.git': [main: 'abc123', develop: 'dev456']
        ], [
                'git@example/app.git@abc123': ['services/app'],
                'git@example/app.git@dev456': ['services/app']
        ])
    }

    private static Map baseJavaDelivery() {
        return [
                application      : [id: 'default-app', type: 'JAVA'],
                targetEnvironment: 'default',
                operationMode    : 'DEPLOY',
                source           : [repository: 'git@example/app.git', reference: 'develop', directory: 'services/app'],
                stages           : [
                        build  : [enabled: true, strategy: 'MAVEN', config: [:]],
                        storage: [enabled: false],
                        deploy : [enabled: false]
                ],
                notification     : [enabled: false]
        ]
    }

    private static Map baseWebDelivery() {
        return [
                application      : [id: 'web-1', type: 'WEB'],
                targetEnvironment: 'test',
                operationMode    : 'DEPLOY',
                source           : [repository: 'git@example/app.git', reference: 'main', directory: 'services/app'],
                stages           : [
                        build  : [enabled: true, strategy: 'NPM', artifact: [path: 'package/web.zip', fileName: 'web.zip'], config: [:]],
                        storage: [enabled: true, target: [type: 'JENKINS_STASH'], config: [archiveType: 'ZIP']],
                        deploy : [enabled: true, strategy: 'WEB_STATIC', nodes: ['web-a'], directArtifactHandoff: false, config: [pathRoot: '/srv/web']]
                ],
                notification     : [enabled: true, message: [wecom: [key: 'test']]]
        ]
    }
}
