package com.daluobai.jenkinslib.delivery

class DeliveryRuntimeFactory {
    static DeliveryRuntime forJenkins(def steps) {
        return new DeliveryRuntime(
                new JenkinsSourceRepositoryAdapter(steps),
                new JenkinsDeliveryStageAdapter(steps),
                new JenkinsArtifactStore(steps),
                new JenkinsDeliveryExecutionCoordinator(steps)
        )
    }
}
