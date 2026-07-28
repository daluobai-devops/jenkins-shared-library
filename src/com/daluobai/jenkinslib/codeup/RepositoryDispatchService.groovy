package com.daluobai.jenkinslib.codeup

import com.daluobai.jenkinslib.api.CodeupApi
import com.daluobai.jenkinslib.delivery.ApplicationDeliveryService
import com.daluobai.jenkinslib.delivery.DeliveryRuntime
import com.daluobai.jenkinslib.delivery.DeliveryExecutionException
import groovy.json.JsonOutput
import com.daluobai.jenkinslib.steps.StepsJenkins

class RepositoryDispatchService implements Serializable {
    static final Set<String> REMOTE_COMMAND_FIELDS = [
            'buildCMD', 'afterRunCMD', 'command', 'lifecycle', 'runOptions', 'runArgs', 'buildArgs'
    ] as Set<String>

    private final def steps
    private final CodeupRepositoryAdapter codeup
    private final DeliveryRuntime deliveryRuntime
    private final JenkinsfileInvocationParser parser = new JenkinsfileInvocationParser()

    RepositoryDispatchService(def steps, CodeupRepositoryAdapter codeup, DeliveryRuntime deliveryRuntime) {
        this.steps = steps
        this.codeup = codeup
        this.deliveryRuntime = deliveryRuntime
    }

    Map dispatch(Map config) {
        validateRequired(config)
        String domain = config.domain?.toString() ?: CodeupApi.DEFAULT_DOMAIN
        Set<String> allowedDomains = ((config.allowedDomains ?: [CodeupApi.DEFAULT_DOMAIN]) as Collection)
                .collect { normalizeDomainHost(it?.toString()) } as Set<String>
        validateDomain(domain, allowedDomains)
        boolean allowAll = config.allowAllRepositories == true
        boolean hasAllowedNames = config.containsKey('allowedRepositoryNames')
        if (!hasAllowedNames && !allowAll) {
            throw new IllegalArgumentException('必须配置allowedRepositoryNames；如确需扫描全部仓库，请显式设置allowAllRepositories=true')
        }
        Set<String> allowedNames = hasAllowedNames
                ? collection(config.allowedRepositoryNames, 'allowedRepositoryNames') as Set<String>
                : [] as Set<String>
        Set<String> allowedMethods = collection(config.allowedMethods ?: ['deployJavaWeb', 'deployWeb'], 'allowedMethods') as Set<String>
        Set<String> supportedMethods = ['deployJavaWeb', 'deployWeb', 'deliverApplication'] as Set<String>
        Set<String> unsupported = allowedMethods.findAll { !supportedMethods.contains(it) } as Set<String>
        if (!unsupported.isEmpty()) {
            throw new IllegalArgumentException("存在不支持的allowedMethods: ${unsupported.join(', ')}")
        }
        Set<String> allowedCommands = collection(config.allowedCommandValues ?: [], 'allowedCommandValues') as Set<String>
        if (config.allowUnsafeCommandFields == true) {
            throw new IllegalArgumentException('allowUnsafeCommandFields已禁用；远端命令必须通过allowedCommandValues精确授权')
        }
        String ref = config.ref?.toString() ?: 'master'
        String fileName = config.jenkinsfileName?.toString()?.trim() ?: 'Jenkinsfile.groovy'
        Map connection = [domain: domain, token: config.token, organizationId: config.organizationId]
        Map summary = [
                scannedRepositories: 0, scannedFiles: 0, preflightPassed: 0, dispatched: 0,
                succeeded: 0, skipped: 0, withdrawn: 0, rejected: [], failed: [], units: []
        ]

        List<Map<String, Object>> repositories = codeup.listRepositories(connection)
        summary.scannedRepositories = repositories.size()
        List<Map> plans = []
        repositories.sort { Map left, Map right -> repositoryName(left) <=> repositoryName(right) }
                .each { Map repository ->
                    if (!allowAll && !allowedNames.contains(repository.name?.toString())) {
                        Map entry = unit(repository, repositoryName(repository), null, 'SKIPPED', '仓库未获目标环境授权', ref)
                        summary.skipped++
                        summary.units.add(entry)
                        return
                    }
                    discoverRepository(repository, connection, ref, fileName, allowedMethods, allowedCommands, plans, summary)
                }

        rejectDuplicateApplications(plans, summary)
        plans.findAll { it.rejected != true }.sort { Map left, Map right -> left.entry.unitId <=> right.entry.unitId }.each { Map plan ->
            executePlan(plan, config.dryRun == true, summary)
        }
        summary.units.sort { Map left, Map right -> left.unitId <=> right.unitId }
        recordSummary(summary)

        if (!summary.failed.isEmpty() || !summary.rejected.isEmpty()) {
            markBuildFailed()
            if (config.failAtEnd == true) {
                steps.error("Codeup仓库分发完成，但存在失败或拒绝项。失败: ${summary.failed.size()}，拒绝: ${summary.rejected.size()}")
            }
        }
        return summary
    }

    private void recordSummary(Map summary) {
        try {
            if (steps.currentBuild != null) {
                steps.currentBuild.description = JsonOutput.toJson(summary)
            }
        } catch (MissingPropertyException ignored) {
            // 纯 Groovy 兼容测试没有 Jenkins currentBuild 全局变量。
        }
    }

    private void markBuildFailed() {
        try {
            if (steps.currentBuild != null) {
                steps.currentBuild.result = 'FAILURE'
            }
        } catch (MissingPropertyException ignored) {
            // 纯 Groovy 兼容测试没有 Jenkins currentBuild 全局变量。
        }
    }

    private void discoverRepository(Map repository,
                                   Map connection,
                                   String ref,
                                   String fileName,
                                   Set<String> allowedMethods,
                                   Set<String> allowedCommands,
                                   List<Map> plans,
                                   Map summary) {
        String repositoryName = repositoryName(repository)
        try {
            List<Map<String, Object>> files = codeup.listFiles(connection, repository, ref)
            files.findAll { Map file ->
                String path = file.path?.toString() ?: file.name?.toString()
                return path?.tokenize('/')?.last() == fileName && file.type?.toString() != 'tree'
            }.sort { Map left, Map right ->
                (left.path?.toString() ?: left.name?.toString()) <=> (right.path?.toString() ?: right.name?.toString())
            }.each { Map file ->
                discoverUnit(repository, repositoryName, file, connection, ref, allowedMethods, allowedCommands, plans, summary)
            }
        } catch (Throwable failure) {
            Map entry = unit(repository, repositoryName, null, 'FAILED', "repository failed: ${failure.message}", ref)
            summary.failed.add(entry)
            summary.units.add(entry)
        }
    }

    private void discoverUnit(Map repository,
                             String repositoryName,
                             Map file,
                             Map connection,
                             String ref,
                             Set<String> allowedMethods,
                             Set<String> allowedCommands,
                             List<Map> plans,
                             Map summary) {
        String path = file.path?.toString() ?: file.name?.toString()
        summary.scannedFiles++
        Map read
        try {
            read = codeup.readFile(connection, repository, path, ref)
        } catch (Throwable failure) {
            read = [status: 'FAILED', reason: failure.message, revision: ref]
        }
        String revision = read.revision?.toString() ?: ref
        if (read.status == 'WITHDRAWN') {
            Map entry = unit(repository, repositoryName, path, 'WITHDRAWN', null, revision)
            summary.withdrawn++
            summary.units.add(entry)
            return
        }
        if (read.status != 'FOUND') {
            Map entry = unit(repository, repositoryName, path, 'FAILED', read.reason?.toString() ?: '配置读取失败', revision)
            summary.failed.add(entry)
            summary.units.add(entry)
            return
        }
        try {
            Map invocation = parser.parse(read.content?.toString(), allowedMethods)
            validateRemoteConfig(invocation.customConfig, allowedCommands)
            Map plan = [invocation: invocation, entry: unit(repository, repositoryName, path, 'DISCOVERED', null, revision)]
            if (invocation.methodName == 'deliverApplication') {
                Map request = new LinkedHashMap(invocation.customConfig as Map)
                request.provenance = (request.provenance ?: [:]) + [
                        repository: repositoryName, configurationPath: path, configurationRevision: revision
                ]
                ApplicationDeliveryService delivery = new ApplicationDeliveryService(deliveryRuntime)
                Map preflight = delivery.preflight(request)
                plan.request = request
                plan.preflight = preflight
                plan.applicationId = preflight.effectiveConfig.DELIVERY.application.id?.toString()
            } else {
                String applicationType = invocation.methodName == 'deployJavaWeb' ? 'JAVA' : 'WEB'
                if (!invocation.customConfig?.SHARE_PARAM?.appName?.toString()?.trim()) {
                    throw new IllegalArgumentException('仓库分发中的旧入口声明必须显式配置SHARE_PARAM.appName')
                }
                try {
                    Map request = new com.daluobai.jenkinslib.delivery.LegacyDeliveryConfigAdapter(steps)
                            .requestFor(applicationType, invocation.customConfig as Map)
                    DeliveryRuntime legacyRuntime = new DeliveryRuntime(
                            new com.daluobai.jenkinslib.delivery.LegacySourceRepositoryAdapter(),
                            deliveryRuntime.stages,
                            deliveryRuntime.artifactStore,
                            deliveryRuntime.coordinator
                    )
                    Map preflight = new ApplicationDeliveryService(legacyRuntime).preflight(request)
                    plan.legacyRequest = request
                    plan.legacyPreflight = preflight
                    plan.applicationId = preflight.effectiveConfig.DELIVERY.application.id?.toString()
                } catch (MissingMethodException unsupportedOutsideJenkins) {
                    if (steps.class.name.contains('WorkflowScript')) {
                        throw unsupportedOutsideJenkins
                    }
                    plan.applicationId = invocation.customConfig?.SHARE_PARAM?.appName?.toString()
                }
            }
            plans.add(plan)
        } catch (IllegalArgumentException failure) {
            Map entry = unit(repository, repositoryName, path, 'REJECTED', failure.message, revision)
            summary.rejected.add(entry)
            summary.units.add(entry)
        } catch (Throwable failure) {
            Map entry = unit(repository, repositoryName, path, 'FAILED', failure.message, revision)
            summary.failed.add(entry)
            summary.units.add(entry)
        }
    }

    private void rejectDuplicateApplications(List<Map> plans, Map summary) {
        plans.findAll { it.applicationId?.toString()?.trim() }
                .groupBy { it.applicationId.toString() }
                .findAll { String ignored, List<Map> matches -> matches.size() > 1 }
                .each { String applicationId, List<Map> matches ->
                    matches.each { Map plan ->
                        plan.rejected = true
                        plan.entry.status = 'REJECTED'
                        plan.entry.reason = "目标环境内应用标识冲突: ${applicationId}"
                        summary.rejected.add(plan.entry)
                        summary.units.add(plan.entry)
                    }
                }
    }

    private void executePlan(Map plan, boolean dryRun, Map summary) {
        Map entry = plan.entry as Map
        try {
            if (dryRun) {
                entry.status = 'PREFLIGHT_PASSED'
                Map preflight = (plan.preflight ?: plan.legacyPreflight ?: [:]) as Map
                entry.deliveryResult = [
                        status: 'PREFLIGHT_PASSED', sourceReference: preflight.sourceReference,
                        resolvedSourceRevision: preflight.resolvedSourceRevision,
                        enabledStages: preflight.stages?.findAll { k, v -> v == true }?.keySet()?.toList()
                ]
                summary.preflightPassed++
            } else {
                Map invocation = plan.invocation as Map
                Map deliveryResult
                if (invocation.methodName == 'deliverApplication') {
                    deliveryResult = new ApplicationDeliveryService(deliveryRuntime)
                            .deliverPrepared(plan.request as Map, plan.preflight as Map)
                } else {
                    deliveryResult = executePreparedLegacy(plan)
                }
                entry.deliveryResult = deliveryResult
                entry.status = 'SUCCESS'
                summary.dispatched++
                summary.succeeded++
            }
            summary.units.add(entry)
        } catch (IllegalArgumentException failure) {
            entry.status = 'REJECTED'
            entry.reason = failure.message
            summary.rejected.add(entry)
            summary.units.add(entry)
        } catch (Throwable failure) {
            entry.status = 'FAILED'
            entry.reason = failure.message
            if (failure instanceof DeliveryExecutionException) {
                entry.deliveryResult = (failure as DeliveryExecutionException).deliveryResult
            }
            summary.failed.add(entry)
            summary.units.add(entry)
        }
    }

    private Map executePreparedLegacy(Map plan) {
        if (!(plan.legacyRequest instanceof Map) || !(plan.legacyPreflight instanceof Map)) {
            // 仅供无 Jenkins DSL 的历史单元测试兼容；真实 Jenkins 必须在发现阶段形成 prepared plan。
            return steps.invokeMethod(plan.invocation.methodName.toString(), [plan.invocation.customConfig as Map] as Object[]) as Map
        }
        List<String> nodes = new StepsJenkins(steps).getNodeByLabel('buildNode') as List<String>
        if (nodes == null || nodes.isEmpty()) {
            steps.error('没有可用的buildNode节点')
        }
        DeliveryRuntime legacyRuntime = new DeliveryRuntime(
                new com.daluobai.jenkinslib.delivery.LegacySourceRepositoryAdapter(),
                new com.daluobai.jenkinslib.delivery.LegacyJenkinsDeliveryStageAdapter(steps),
                new com.daluobai.jenkinslib.delivery.JenkinsArtifactStore(steps),
                new com.daluobai.jenkinslib.delivery.JenkinsDeliveryExecutionCoordinator(steps)
        )
        return steps.node(nodes.first()) {
            return new ApplicationDeliveryService(legacyRuntime).deliverPrepared(
                    plan.legacyRequest as Map, plan.legacyPreflight as Map, false)
        }
    }

    private static void validateRequired(Map config) {
        if (!config.token?.toString()?.trim()) {
            throw new IllegalArgumentException('token空的')
        }
        if (!config.organizationId?.toString()?.trim()) {
            throw new IllegalArgumentException('organizationId空的')
        }
    }

    private static Collection<String> collection(Object value, String name) {
        if (!(value instanceof Collection)) {
            throw new IllegalArgumentException("${name}必须是集合")
        }
        return (value as Collection).collect { it?.toString() }
    }

    private static String repositoryName(Map repository) {
        return (repository.name ?: repository.path ?: repository.pathWithNamespace ?: repository.id).toString()
    }

    private static Map unit(Map repository, String repositoryName, String path, String status, String reason, String revision) {
        return [
                unitId               : path == null ? repositoryName : "${repositoryName}:${path}".toString(),
                repositoryId         : repository.id?.toString(),
                repositoryName       : repositoryName,
                configurationPath    : path,
                configurationRevision: revision,
                status               : status,
                reason               : reason
        ]
    }

    private static void validateRemoteConfig(Object value, Set<String> allowedCommands, String path = 'customConfig') {
        if (value instanceof Map) {
            (value as Map).each { Object key, Object nested ->
                String current = "${path}.${key}".toString()
                String field = key?.toString()
                if (field == 'subModule') {
                    String module = nested?.toString()
                    if (!module || !(module ==~ /[A-Za-z0-9][A-Za-z0-9._\/-]*/) || module.tokenize('/').contains('..')) {
                        throw new IllegalArgumentException("远端配置字段${current}不是安全的Maven模块相对路径")
                    }
                } else if (field == 'activeProfile') {
                    if (!(nested?.toString() ==~ /[A-Za-z0-9][A-Za-z0-9,._-]*/)) {
                        throw new IllegalArgumentException("远端配置字段${current}不是安全的Maven Profile列表")
                    }
                } else if (field == 'skipTest') {
                    if (!(nested instanceof Boolean) && !(nested?.toString()?.toLowerCase(Locale.ROOT) in ['true', 'false'])) {
                        throw new IllegalArgumentException("远端配置字段${current}必须是布尔值")
                    }
                } else if (REMOTE_COMMAND_FIELDS.contains(field)) {
                    String command = nested?.toString()
                    if (command?.trim() && !allowedCommands.contains(command)) {
                        throw new IllegalArgumentException("远端配置字段${current}包含命令内容，必须通过allowedCommandValues精确授权")
                    }
                } else {
                    validateRemoteConfig(nested, allowedCommands, current)
                }
            }
        } else if (value instanceof Collection) {
            (value as Collection).eachWithIndex { Object nested, int index ->
                validateRemoteConfig(nested, allowedCommands, "${path}[${index}]".toString())
            }
        } else if (value instanceof CharSequence) {
            String text = value.toString()
            if (['\u0000', '\r', '\n', ';', '&', '|', '`', '$(', '>', '<', "'", '"'].any { text.contains(it) }) {
                throw new IllegalArgumentException("远端配置字段${path}包含未授权的Shell字符")
            }
        }
    }

    private static void validateDomain(String domain, Set<String> allowedDomains) {
        String normalized = domain.contains('://') ? domain : "https://${domain}"
        URI uri
        try {
            uri = new URI(normalized)
        } catch (Throwable ignored) {
            throw new IllegalArgumentException('domain格式不正确')
        }
        if (!uri.scheme?.equalsIgnoreCase('https') || !uri.host || uri.userInfo != null) {
            throw new IllegalArgumentException('domain必须是无用户信息的HTTPS地址')
        }
        if (!allowedDomains.contains(uri.host.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException("domain不在allowedDomains白名单中: ${uri.host}")
        }
    }

    private static String normalizeDomainHost(String domain) {
        if (domain == null) {
            return ''
        }
        String normalized = domain.contains('://') ? domain : "https://${domain}"
        try {
            return new URI(normalized).host?.toLowerCase(Locale.ROOT) ?: ''
        } catch (Throwable ignored) {
            return ''
        }
    }
}
