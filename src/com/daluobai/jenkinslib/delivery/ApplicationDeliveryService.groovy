package com.daluobai.jenkinslib.delivery

class ApplicationDeliveryService implements Serializable {
    private final DeliveryRuntime runtime
    private final EffectiveDeliveryConfigResolver resolver = new EffectiveDeliveryConfigResolver()

    ApplicationDeliveryService(DeliveryRuntime runtime) {
        this.runtime = runtime
    }

    Map preflight(Map request) {
        Map resolved = resolver.resolve(request ?: [:])
        Map effectiveConfig = resolved.effectiveConfig as Map
        Map result = new DeliveryPreflight(runtime.sourceRepository, runtime.stages).validate(
                effectiveConfig,
                request.legacyCompatibility == true && request.legacyNoop == true
        )
        result.effectiveConfig = effectiveConfig
        result.configurationSources = resolved.configurationSources
        result.configurationProvenance = request.provenance ?: [:]
        return result
    }

    Map deliver(Map request) {
        return deliver(request, false)
    }

    Map deliver(Map request, boolean preserveFailureType) {
        Map preflight = preflight(request)
        return deliverPrepared(request, preflight, preserveFailureType)
    }

    Map deliverPrepared(Map request, Map preflight, boolean preserveFailureType = false) {
        Map effectiveConfig = preflight.effectiveConfig as Map
        Map execution = (request.execution ?: [:]) as Map
        Map delivery = effectiveConfig.DELIVERY as Map
        Map result = [
                status                : 'RUNNING',
                applicationId         : delivery.application.id.toString(),
                targetEnvironment     : delivery.targetEnvironment.toString(),
                sourceRepository      : delivery.source.repository.toString(),
                sourceReference       : preflight.sourceReference,
                resolvedSourceRevision: preflight.resolvedSourceRevision,
                configurationSources  : preflight.configurationSources,
                configurationProvenance: preflight.configurationProvenance,
                enabledStages         : preflight.stages.findAll { String ignored, boolean enabled -> enabled }.keySet().toList(),
                deploymentNodes       : preflight.stages.deploy == true ? (delivery.stages.deploy.nodes ?: []).toList() : []
        ]

        result.warnings = []
        Throwable primaryFailure = null
        try {
            runtime.coordinator.beforeExecution(execution, effectiveConfig)
            runtime.stages.initializeWorkspace(effectiveConfig)
            notifySafely(effectiveConfig, 'STARTED', result)
            Map artifact = [:]
            if (preflight.stages.build == true) {
                runtime.sourceRepository.checkout(delivery.source as Map, preflight.resolvedSourceRevision.toString())
                artifact = runtime.stages.build(effectiveConfig, preflight)
                result.artifact = artifact
            }
            if (preflight.stages.storage == true) {
                artifact = runtime.stages.store(effectiveConfig, preflight, artifact)
                String identity = [
                        result.targetEnvironment,
                        result.applicationId,
                        execution.id
                ].join('/')
                artifact = runtime.artifactStore.put(identity, artifact)
                result.artifact = artifact
            }
            if (preflight.stages.deploy == true) {
                result.deploymentStarted = true
                runtime.coordinator.deploymentStarted(execution, result)
                runtime.stages.deploy(effectiveConfig, preflight, artifact)
            }
            result.artifact = artifact
            result.status = 'SUCCESS'
            notifySafely(effectiveConfig, 'SUCCESS', result)
        } catch (Throwable failure) {
            primaryFailure = failure
            result.status = failure.class.name == 'org.jenkinsci.plugins.workflow.steps.FlowInterruptedException'
                    ? 'ABORTED'
                    : 'FAILED'
            result.failure = [type: failure.class.name, message: failure.message]
            notifySafely(effectiveConfig, result.status.toString(), result)
        } finally {
            try {
                runtime.stages.cleanupWorkspace(effectiveConfig)
            } catch (Throwable cleanupFailure) {
                if (primaryFailure == null) {
                    primaryFailure = cleanupFailure
                    result.status = 'FAILED'
                    result.failure = [type: cleanupFailure.class.name, message: cleanupFailure.message]
                } else {
                    addWarning(result, cleanupFailure)
                }
            }
            try {
                runtime.coordinator.afterExecution(execution, result)
            } catch (Throwable recordFailure) {
                addWarning(result, recordFailure)
            }
        }
        if (primaryFailure != null) {
            if (preserveFailureType) {
                throw primaryFailure
            }
            throw new DeliveryExecutionException(primaryFailure, result)
        }
        return result
    }

    private void notifySafely(Map effectiveConfig, String status, Map result) {
        if (effectiveConfig.DELIVERY.notification?.enabled != true) {
            return
        }
        try {
            runtime.stages.notify(effectiveConfig, status, result)
        } catch (Throwable notificationFailure) {
            addWarning(result, notificationFailure)
        }
    }

    private static void addWarning(Map result, Throwable failure) {
        result.warnings.add([type: failure.class.name, message: failure.message])
    }
}
