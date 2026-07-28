package com.daluobai.jenkinslib.delivery

interface DeliveryExecutionCoordinator extends Serializable {
    void beforeExecution(Map execution, Map effectiveConfig)

    void deploymentStarted(Map execution, Map result)

    void afterExecution(Map execution, Map result)
}
