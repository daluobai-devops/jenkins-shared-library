package com.daluobai.jenkinslib.delivery

class InMemoryDeliveryStageAdapter implements DeliveryStageAdapter {
    final List<String> events = []
    Map buildArtifact = [path: 'build/application.jar', fileName: 'application.jar', checksum: 'sha256:test']
    Closure beforeBuild = { -> }
    RuntimeException buildFailure
    String deploymentFailureNode
    RuntimeException cleanupFailure
    final Map<String, RuntimeException> notificationFailures = [:]
    final Set<String> unavailableNodes = [] as Set<String>

    @Override
    boolean nodesAvailable(Collection nodes) {
        return nodes.every { !unavailableNodes.contains(it?.toString()) }
    }

    @Override
    void initializeWorkspace(Map effectiveConfig) {
        events.add('initialize')
    }

    @Override
    Map build(Map effectiveConfig, Map preflight) {
        beforeBuild.call()
        events.add("build:${effectiveConfig.DELIVERY.application.type}:${effectiveConfig.DELIVERY.stages.build.strategy}:${preflight.resolvedSourceRevision}".toString())
        if (buildFailure != null) {
            throw buildFailure
        }
        return new LinkedHashMap(buildArtifact)
    }

    @Override
    Map store(Map effectiveConfig, Map preflight, Map artifact) {
        events.add('store')
        return artifact
    }

    @Override
    void deploy(Map effectiveConfig, Map preflight, Map artifact) {
        Map deploy = effectiveConfig.DELIVERY.stages.deploy as Map
        (deploy.nodes as Collection).each { Object node ->
            String nodeName = node.toString()
            events.add("deploy:${nodeName}".toString())
            if (deploymentFailureNode == nodeName) {
                throw new IllegalStateException("deployment failed: ${nodeName}")
            }
            ((deploy.readiness ?: []) as Collection).each { Object check ->
                events.add("ready:${nodeName}:${check.type}".toString())
            }
        }
    }

    @Override
    void notify(Map effectiveConfig, String status, Map result) {
        events.add("notify:${status}".toString())
        if (notificationFailures[status] != null) {
            throw notificationFailures[status]
        }
    }

    @Override
    void cleanupWorkspace(Map effectiveConfig) {
        events.add('cleanup')
        if (cleanupFailure != null) {
            throw cleanupFailure
        }
    }
}
