package com.daluobai.jenkinslib.delivery

class DeliveryRuntime implements Serializable {
    final SourceRepositoryAdapter sourceRepository
    final DeliveryStageAdapter stages
    final ArtifactStore artifactStore
    final DeliveryExecutionCoordinator coordinator

    DeliveryRuntime(SourceRepositoryAdapter sourceRepository,
                    DeliveryStageAdapter stages,
                    ArtifactStore artifactStore,
                    DeliveryExecutionCoordinator coordinator) {
        this.sourceRepository = sourceRepository
        this.stages = stages
        this.artifactStore = artifactStore
        this.coordinator = coordinator
    }
}
