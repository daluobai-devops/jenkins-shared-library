package com.daluobai.jenkinslib.delivery

class DeliveryPreflight implements Serializable {
    private final SourceRepositoryAdapter sourceRepository
    private final DeliveryStageAdapter stages

    DeliveryPreflight(SourceRepositoryAdapter sourceRepository, DeliveryStageAdapter stages) {
        this.sourceRepository = sourceRepository
        this.stages = stages
    }

    Map validate(Map effectiveConfig, boolean allowLegacyNoop = false) {
        Map delivery = effectiveConfig.DELIVERY as Map
        if (!delivery) {
            throw new IllegalArgumentException('DELIVERY统一交付配置为空')
        }
        if (!delivery.application?.id) {
            throw new IllegalArgumentException('应用标识为空')
        }
        if (!(delivery.application?.type in ['JAVA', 'WEB'])) {
            throw new IllegalArgumentException('应用类型仅支持JAVA或WEB')
        }
        if (!delivery.targetEnvironment) {
            throw new IllegalArgumentException('目标环境为空')
        }
        String operationMode = delivery.operationMode?.toString() ?: 'DEPLOY'
        if (operationMode != 'DEPLOY') {
            throw new IllegalArgumentException("不支持的操作模式: ${operationMode}")
        }
        Map build = delivery.stages?.build as Map
        Map storage = delivery.stages?.storage as Map
        Map deploy = delivery.stages?.deploy as Map
        boolean buildEnabled = build?.enabled == true
        boolean storageEnabled = storage?.enabled == true
        boolean deployEnabled = deploy?.enabled == true
        if (allowLegacyNoop && !buildEnabled && !storageEnabled && !deployEnabled) {
            return [
                    resolvedSourceRevision: delivery.source?.reference?.toString() ?: 'legacy',
                    sourceReference       : delivery.source?.reference?.toString() ?: 'legacy',
                    stages               : [build: false, storage: false, deploy: false]
            ]
        }
        if (!buildEnabled) {
            throw new IllegalArgumentException(storageEnabled || deployEnabled ? '产物存储和部署要求启用构建阶段' : '空交付配置无效')
        }
        String buildStrategy = build.strategy?.toString()
        Collection allowedBuildStrategies = delivery.application.type == 'JAVA' ? ['MAVEN'] : ['NPM']
        if (!allowedBuildStrategies.contains(buildStrategy)) {
            throw new IllegalArgumentException("应用类型${delivery.application.type}不支持构建策略${buildStrategy}")
        }
        Collection storageTargets = storage?.targets instanceof Collection
                ? storage.targets as Collection
                : (storage?.target ? [storage.target] : [])
        if (storageEnabled && storageTargets.size() != 1) {
            throw new IllegalArgumentException('产物存储必须且只能选择一个目标')
        }
        String storageType = storageEnabled ? (storageTargets.first() as Map).type?.toString() : null
        if (storageEnabled && !(storageType in ['JENKINS_STASH', 'JENKINS', 'JENKINS_ARCHIVE'])) {
            throw new IllegalArgumentException("不支持的产物存储目标: ${storageType}")
        }
        if (storageType == 'JENKINS_STASH' && !(storage.config?.archiveType?.toString()?.toUpperCase(Locale.ROOT) in ['JAR', 'WAR', 'TAR', 'ZIP'])) {
            throw new IllegalArgumentException('JENKINS_STASH要求archiveType为JAR、WAR、TAR或ZIP')
        }
        String artifactPath = build.artifact?.path?.toString()
        if (artifactPath && (!(artifactPath ==~ /[A-Za-z0-9._*?\/-]+/) || artifactPath.contains('..'))) {
            throw new IllegalArgumentException('构建产物路径包含不安全字符或越界片段')
        }
        if (deployEnabled) {
            Collection strategies = deploy?.strategies instanceof Collection
                    ? deploy.strategies as Collection
                    : (deploy?.strategy ? [deploy.strategy] : [])
            if (strategies.size() != 1) {
                throw new IllegalArgumentException('部署必须且只能选择一个策略')
            }
            String strategy = strategies.first().toString()
            Collection allowedStrategies = delivery.application.type == 'JAVA'
                    ? ['JAVA_SERVICE', 'TOMCAT']
                    : ['WEB_STATIC']
            if (!allowedStrategies.contains(strategy)) {
                throw new IllegalArgumentException("应用类型${delivery.application.type}不支持部署策略${strategy}")
            }
            if (!(deploy.nodes instanceof Collection) || deploy.nodes.isEmpty()) {
                throw new IllegalArgumentException('部署节点为空')
            }
            if (!stages.nodesAvailable(deploy.nodes as Collection)) {
                throw new IllegalArgumentException('存在不可用的部署节点')
            }
            if (!storageEnabled && deploy.directArtifactHandoff != true) {
                throw new IllegalArgumentException('部署产物不可达：未启用存储且未声明直接产物交接')
            }
            if (storageEnabled && storageType != 'JENKINS_STASH') {
                throw new IllegalArgumentException('部署要求使用JENKINS_STASH存储目标，或关闭存储并启用直接产物交接')
            }
            if (delivery.application.type == 'WEB' && !deploy.config?.pathRoot) {
                throw new IllegalArgumentException('Web部署目标目录为空')
            }
            if (delivery.application.type == 'WEB') {
                String fileName = build.artifact?.fileName?.toString()
                String normalizedFileName = fileName?.toLowerCase(Locale.ROOT)
                String artifactFormat = normalizedFileName?.endsWith('.zip') ? 'ZIP' :
                        ((normalizedFileName?.endsWith('.tar') || normalizedFileName?.endsWith('.tar.gz')) ? 'TAR' : null)
                if (!(artifactFormat in ['ZIP', 'TAR'])) {
                    throw new IllegalArgumentException('Web部署产物格式仅支持ZIP或TAR')
                }
            }
            ((deploy.readiness ?: []) as Collection).each { Object check ->
                if (!(check?.type in ['TCP', 'HTTP', 'COMMAND'])) {
                    throw new IllegalArgumentException("不支持的就绪验证: ${check?.type}")
                }
            }
        }
        Map source = delivery.source as Map
        if (!source?.repository || !source?.reference || !source?.directory) {
            throw new IllegalArgumentException('源码仓库、引用和单元源码目录均为必填项')
        }
        String sourceDirectory = source.directory.toString()
        if (!(sourceDirectory ==~ /[A-Za-z0-9._\/-]+/) || sourceDirectory.startsWith('/') ||
                sourceDirectory == '..' || sourceDirectory.tokenize('/').contains('..')) {
            throw new IllegalArgumentException('单元源码目录必须是安全的仓库内相对路径')
        }
        String revision = sourceRepository.resolveRevision(source)
        if (!sourceRepository.directoryExists(source, revision)) {
            throw new IllegalArgumentException("已解析源码版本中不存在单元源码目录: ${source.directory}")
        }
        return [
                resolvedSourceRevision: revision,
                sourceReference       : source.reference.toString(),
                stages                : [build: buildEnabled, storage: storageEnabled, deploy: deployEnabled]
        ]
    }
}
