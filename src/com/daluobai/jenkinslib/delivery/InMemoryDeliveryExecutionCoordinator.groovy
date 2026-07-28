package com.daluobai.jenkinslib.delivery

class InMemoryDeliveryExecutionCoordinator implements DeliveryExecutionCoordinator {
    final List<String> events = []
    final List<Map> supersessions = []
    Map activeExecution

    InMemoryDeliveryExecutionCoordinator(Map activeExecution = null) {
        this.activeExecution = activeExecution
    }

    @Override
    void deploymentStarted(Map execution, Map result) {
        if (activeExecution?.id == execution.id) {
            activeExecution.deploymentStarted = true
        }
    }

    @Override
    void beforeExecution(Map execution, Map effectiveConfig) {
        events.add("before:${execution.id}".toString())
        if (activeExecution != null && activeExecution.id != execution.id) {
            supersessions.add([
                    oldExecution     : activeExecution.id,
                    newExecution     : execution.id,
                    applicationId    : effectiveConfig.DELIVERY.application.id,
                    targetEnvironment: effectiveConfig.DELIVERY.targetEnvironment,
                    deploymentStarted: activeExecution.deploymentStarted == true
            ])
        }
        activeExecution = [
                id               : execution.id,
                applicationId    : effectiveConfig.DELIVERY.application.id,
                targetEnvironment: effectiveConfig.DELIVERY.targetEnvironment,
                deploymentStarted: false
        ]
    }

    @Override
    void afterExecution(Map execution, Map result) {
        events.add("after:${execution.id}:${result.status}".toString())
        if (activeExecution?.id == execution.id) {
            activeExecution = null
        }
    }
}
