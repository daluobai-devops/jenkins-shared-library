import com.daluobai.jenkinslib.api.CodeupApi
import com.daluobai.jenkinslib.codeup.JenkinsfileInvocationParser
import com.daluobai.jenkinslib.utils.AssertUtils

/**
 * 扫描 Codeup 仓库中的 Jenkinsfile.groovy，并执行允许的方法。
 * allowedRepositoryNames 可选，必须是仓库名称集合（按 Codeup 返回的 repository.name 过滤）；
 * 不传时处理全部仓库，传空集合时全部跳过。
 */
def call(Map config = [:]) {
    AssertUtils.notBlank(config.token?.toString(), 'token空的')
    AssertUtils.notBlank(config.organizationId?.toString(), 'organizationId空的')

    String token = config.token.toString()
    String organizationId = config.organizationId.toString()
    String domain = config.domain?.toString() ?: CodeupApi.DEFAULT_DOMAIN
    String ref = config.ref?.toString() ?: 'master'
    boolean dryRun = config.dryRun == true
    boolean failAtEnd = config.failAtEnd == true

    Set<String> allowedMethods = ((config.allowedMethods ?: ['deployJavaWeb']) as Collection).collect { Object item ->
        return item.toString()
    } as Set<String>
    boolean hasAllowedRepositoryNames = config.containsKey('allowedRepositoryNames')
    Set<String> allowedRepositoryNames = null
    if (hasAllowedRepositoryNames) {
        if (!(config.allowedRepositoryNames instanceof Collection)) {
            throw new IllegalArgumentException('allowedRepositoryNames必须是集合')
        }
        allowedRepositoryNames = ((Collection) config.allowedRepositoryNames).collect { Object item ->
            return item?.toString()
        } as Set<String>
    }

    def whitelist = [
            deployJavaWeb: { Map customConfig -> deployJavaWeb(customConfig) }
    ]
    Set<String> unsupportedMethods = allowedMethods.findAll { String methodName ->
        return !whitelist.containsKey(methodName)
    } as Set<String>
    if (!unsupportedMethods.isEmpty()) {
        throw new IllegalArgumentException("存在不支持的allowedMethods: ${unsupportedMethods.join(', ')}")
    }

    CodeupApi codeupApi = new CodeupApi(this)
    JenkinsfileInvocationParser parser = new JenkinsfileInvocationParser()
    Map summary = [
            scannedRepositories: 0,
            scannedFiles       : 0,
            dispatched         : 0,
            rejected           : [],
            failed             : []
    ]

    List<Map<String, Object>> repositories = codeupApi.listRepositories(domain, token, organizationId)
    summary.scannedRepositories = repositories.size()

    repositories.findAll { Map<String, Object> repository ->
        if (!hasAllowedRepositoryNames) {
            return true
        }
        String repositoryName = repository.name?.toString()
        boolean allowed = allowedRepositoryNames.contains(repositoryName)
        if (!allowed) {
            echo "Codeup仓库不在allowedRepositoryNames中，跳过。repositoryName=${repositoryName}"
        }
        return allowed
    }.each { Map<String, Object> repository ->
        processRepository(repository, codeupApi, parser, whitelist, allowedMethods, domain, token, organizationId, ref, dryRun, summary)
    }

    if (failAtEnd && (!summary.failed.isEmpty() || !summary.rejected.isEmpty())) {
        error("Codeup仓库分发完成，但存在失败或拒绝项。失败: ${summary.failed.size()}，拒绝: ${summary.rejected.size()}")
    }
    return summary
}

private void processRepository(Map<String, Object> repository,
                               CodeupApi codeupApi,
                               JenkinsfileInvocationParser parser,
                               Map<String, Closure> whitelist,
                               Set<String> allowedMethods,
                               String domain,
                               String token,
                               String organizationId,
                               String ref,
                               boolean dryRun,
                               Map summary) {
    String repositoryId = repository.id?.toString()
    String repositoryName = repository.name?.toString() ?: repository.path?.toString() ?: repository.pathWithNamespace?.toString() ?: repositoryId
    try {
        AssertUtils.notBlank(repositoryId, 'repositoryId空的')
        List<Map<String, Object>> files = codeupApi.listFiles(domain, token, repositoryId, '', ref, 'RECURSIVE', organizationId)
        List<Map<String, Object>> jenkinsfiles = files.findAll { Map<String, Object> fileItem ->
            String name = fileItem.name?.toString()
            String path = fileItem.path?.toString()
            String type = fileItem.type?.toString()
            boolean fileMatches = name == 'Jenkinsfile.groovy' || path?.endsWith('/Jenkinsfile.groovy') || path == 'Jenkinsfile.groovy'
            boolean notDirectory = type == null || type != 'tree'
            return fileMatches && notDirectory
        }

        jenkinsfiles.each { Map<String, Object> fileItem ->
            processJenkinsfile(repositoryId, repositoryName, fileItem, codeupApi, parser, whitelist, allowedMethods, domain, token, organizationId, ref, dryRun, summary)
        }
    } catch (Exception e) {
        Map<String, Object> failure = buildEntry(repositoryId, repositoryName, null, "repository failed: ${e.message}")
        summary.failed.add(failure)
        echo "Codeup仓库处理失败，跳过继续。${failure}"
    }
}

private void processJenkinsfile(String repositoryId,
                                String repositoryName,
                                Map<String, Object> fileItem,
                                CodeupApi codeupApi,
                                JenkinsfileInvocationParser parser,
                                Map<String, Closure> whitelist,
                                Set<String> allowedMethods,
                                String domain,
                                String token,
                                String organizationId,
                                String ref,
                                boolean dryRun,
                                Map summary) {
    String filePath = fileItem.path?.toString() ?: fileItem.name?.toString() ?: 'Jenkinsfile.groovy'
    summary.scannedFiles = ((summary.scannedFiles ?: 0) as int) + 1
    try {
        String content = codeupApi.getFileContent(domain, token, repositoryId, filePath, ref, organizationId)
        if (content == null || content.trim().isEmpty()) {
            throw new RuntimeException('Jenkinsfile内容为空')
        }

        Map invocation = parser.parse(content, allowedMethods)
        if (dryRun) {
            echo "Codeup Jenkinsfile解析成功（dryRun），repo=${repositoryName}, path=${filePath}, method=${invocation.methodName}"
            return
        }

        whitelist[invocation.methodName](invocation.customConfig as Map)
        summary.dispatched = ((summary.dispatched ?: 0) as int) + 1
        echo "Codeup Jenkinsfile分发成功，repo=${repositoryName}, path=${filePath}, method=${invocation.methodName}"
    } catch (IllegalArgumentException e) {
        Map<String, Object> rejected = buildEntry(repositoryId, repositoryName, filePath, "parse rejected: ${e.message}")
        summary.rejected.add(rejected)
        echo "Codeup Jenkinsfile解析被拒绝，跳过继续。${rejected}"
    } catch (Exception e) {
        Map<String, Object> failure = buildEntry(repositoryId, repositoryName, filePath, "execution failed: ${e.message}")
        summary.failed.add(failure)
        echo "Codeup Jenkinsfile处理失败，跳过继续。${failure}"
    }
}

private static Map<String, Object> buildEntry(String repositoryId, String repositoryName, String filePath, String reason) {
    return [
            repositoryId  : repositoryId,
            repositoryName: repositoryName,
            filePath      : filePath,
            reason        : reason
    ]
}
