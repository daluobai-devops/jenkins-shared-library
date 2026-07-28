package com.daluobai.jenkinslib.delivery

import com.daluobai.jenkinslib.steps.StepsJenkins

class LegacyApplicationDeliveryEntrypoint implements Serializable {
    private final def steps

    LegacyApplicationDeliveryEntrypoint(def steps) {
        this.steps = steps
    }

    Map deliver(String applicationType, Map customConfig) {
        Map request = new LegacyDeliveryConfigAdapter(steps).requestFor(applicationType, customConfig ?: [:])
        DeliveryRuntime runtime = new DeliveryRuntime(
                new LegacySourceRepositoryAdapter(),
                new LegacyJenkinsDeliveryStageAdapter(steps),
                new JenkinsArtifactStore(steps),
                new JenkinsDeliveryExecutionCoordinator(steps)
        )
        List<String> nodes = new StepsJenkins(steps).getNodeByLabel('buildNode') as List<String>
        steps.echo("buildNode可用节点: ${nodes}")
        if (nodes == null || nodes.isEmpty()) {
            steps.error('没有可用的buildNode节点')
        }
        return steps.node(nodes.first()) {
            new ApplicationDeliveryService(runtime).deliver(request, true)
        }
    }
}
