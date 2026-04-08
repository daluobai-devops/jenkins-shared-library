package com.daluobai.jenkinslib.api

import com.daluobai.jenkinslib.utils.AssertUtils
import com.daluobai.jenkinslib.utils.HttpUtils
import com.daluobai.jenkinslib.utils.JsonUtils
import com.daluobai.jenkinslib.utils.StrUtils

import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.Base64

/**
 * @author daluobai@outlook.com
 * version 1.0.0
 * @title Codeup API
 * @description https://github.com/daluobai-devops/jenkins-shared-library
 * @create 2026/4/7
 */
class CodeupApi implements Serializable {
    static final String DEFAULT_DOMAIN = 'openapi-rdc.aliyuncs.com'

    def steps

    CodeupApi(steps) { this.steps = steps }

    boolean fileExists(String token, String repositoryId, String filePath, String ref) {
        return fileExists(DEFAULT_DOMAIN, token, repositoryId, filePath, ref, null)
    }

    boolean fileExists(String domain, String token, String repositoryId, String filePath, String ref) {
        return fileExists(domain, token, repositoryId, filePath, ref, null)
    }

    boolean fileExists(String domain, String token, String repositoryId, String filePath, String ref, String organizationId) {
        HttpUtils.HttpResponse response = doGetFile(domain, token, repositoryId, filePath, ref, organizationId)
        if (response.getStatus() == HttpURLConnection.HTTP_OK) {
            return true
        }
        if (response.getStatus() == HttpURLConnection.HTTP_NOT_FOUND) {
            return false
        }
        throw new RuntimeException("检查Codeup文件是否存在失败，响应码: ${response.getStatus()}")
    }

    String getFileContent(String token, String repositoryId, String filePath, String ref) {
        return getFileContent(DEFAULT_DOMAIN, token, repositoryId, filePath, ref, null)
    }

    String getFileContent(String domain, String token, String repositoryId, String filePath, String ref) {
        return getFileContent(domain, token, repositoryId, filePath, ref, null)
    }

    String getFileContent(String domain, String token, String repositoryId, String filePath, String ref, String organizationId) {
        HttpUtils.HttpResponse response = doGetFile(domain, token, repositoryId, filePath, ref, organizationId)
        if (response.getStatus() == HttpURLConnection.HTTP_NOT_FOUND) {
            return null
        }
        if (!response.isOk()) {
            throw new RuntimeException("查询Codeup文件内容失败，响应码: ${response.getStatus()}")
        }

        Map<String, Object> responseJson = JsonUtils.parseObj(response.body())
        Object content = responseJson.get("content")
        if (content == null) {
            return null
        }

        String contentStr = content.toString()
        String encoding = responseJson.get("encoding")?.toString()
        if (StrUtils.equalsIgnoreCase(encoding, "base64")) {
            return new String(Base64.decoder.decode(contentStr), StandardCharsets.UTF_8)
        }
        return contentStr
    }

    private HttpUtils.HttpResponse doGetFile(String domain, String token, String repositoryId, String filePath, String ref, String organizationId) {
        AssertUtils.notBlank(domain, "domain空的")
        AssertUtils.notBlank(token, "token空的")
        AssertUtils.notBlank(repositoryId, "repositoryId空的")
        AssertUtils.notBlank(filePath, "filePath空的")
        AssertUtils.notBlank(ref, "ref空的")

        String url = buildFileUrl(domain, repositoryId, filePath, ref, organizationId)
        return HttpUtils.HttpRequest.get(url)
                .header("x-yunxiao-token", token)
                .timeout(30000)
                .execute()
    }

    private static String buildFileUrl(String domain, String repositoryId, String filePath, String ref, String organizationId) {
        String normalizedDomain = normalizeDomain(domain)
        String encodedRepositoryId = encodePathSegment(repositoryId)
        String encodedFilePath = encodePathSegment(filePath)
        String encodedRef = URLEncoder.encode(ref, "UTF-8")

        if (StrUtils.isNotBlank(organizationId)) {
            String encodedOrganizationId = encodePathSegment(organizationId)
            return "${normalizedDomain}/oapi/v1/codeup/organizations/${encodedOrganizationId}/repositories/${encodedRepositoryId}/files/${encodedFilePath}?ref=${encodedRef}"
        }
        return "${normalizedDomain}/oapi/v1/codeup/repositories/${encodedRepositoryId}/files/${encodedFilePath}?ref=${encodedRef}"
    }

    private static String normalizeDomain(String domain) {
        String normalized = domain.trim()
        if (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1)
        }
        if (!normalized.startsWith("http://") && !normalized.startsWith("https://")) {
            normalized = "https://${normalized}"
        }
        return normalized
    }

    private static String encodePathSegment(String value) {
        return URLEncoder.encode(value, "UTF-8").replace("+", "%20")
    }
}
