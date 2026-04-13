package com.daluobai.jenkinslib.api

import com.daluobai.jenkinslib.utils.HttpUtils
import com.daluobai.jenkinslib.utils.JsonUtils
import groovy.lang.ExpandoMetaClass
import groovy.lang.GroovySystem
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertFalse
import static org.junit.jupiter.api.Assertions.assertNotNull
import static org.junit.jupiter.api.Assertions.assertNull
import static org.junit.jupiter.api.Assertions.assertThrows
import static org.junit.jupiter.api.Assertions.assertTrue

class CodeupApiTest {

    @AfterEach
    void cleanup() {
        GroovySystem.metaClassRegistry.removeMetaClass(HttpUtils.HttpRequest)
    }

    @Test
    void fileExistsReturnsTrueWhenCodeupReturns200() {
        Map<String, Object> captured = [:]
        stubGetRequest(captured, new HttpUtils.HttpResponse(HttpURLConnection.HTTP_OK, '{"content":"dGVzdA==","encoding":"base64"}'))

        CodeupApi api = new CodeupApi(null)

        boolean exists = api.fileExists('pt-token', 'group/demo', 'src/main.txt', 'master')

        assertTrue(exists)
        assertEquals('https://openapi-rdc.aliyuncs.com/oapi/v1/codeup/repositories/group%2Fdemo/files/src%2Fmain.txt?ref=master', captured.url)
        assertEquals('pt-token', captured.headers['x-yunxiao-token'])
    }

    @Test
    void fileExistsSupportsCustomDomainWhenProvided() {
        Map<String, Object> captured = [:]
        stubGetRequest(captured, new HttpUtils.HttpResponse(HttpURLConnection.HTTP_OK, '{"content":"dGVzdA==","encoding":"base64"}'))

        CodeupApi api = new CodeupApi(null)

        boolean exists = api.fileExists('codeup.example.com', 'pt-token', 'group/demo', 'src/main.txt', 'master')

        assertTrue(exists)
        assertEquals('https://codeup.example.com/oapi/v1/codeup/repositories/group%2Fdemo/files/src%2Fmain.txt?ref=master', captured.url)
    }

    @Test
    void fileExistsReturnsFalseWhenCodeupReturns404() {
        stubGetRequest([:], new HttpUtils.HttpResponse(HttpURLConnection.HTTP_NOT_FOUND, ''))

        CodeupApi api = new CodeupApi(null)

        assertFalse(api.fileExists('pt-token', '2813489', 'missing.txt', 'master'))
    }

    @Test
    void getFileContentDecodesBase64Content() {
        Map<String, Object> captured = [:]
        stubGetRequest(captured, new HttpUtils.HttpResponse(HttpURLConnection.HTTP_OK, '{"content":"5L2g5aW9","encoding":"base64"}'))

        CodeupApi api = new CodeupApi(null)

        assertEquals('你好', api.getFileContent('pt-token', '2813489', 'demo.txt', 'master'))
        assertEquals('https://openapi-rdc.aliyuncs.com/oapi/v1/codeup/repositories/2813489/files/demo.txt?ref=master', captured.url)
    }

    @Test
    void getFileContentSupportsCustomDomainWhenProvided() {
        Map<String, Object> captured = [:]
        stubGetRequest(captured, new HttpUtils.HttpResponse(HttpURLConnection.HTTP_OK, '{"content":"aGVsbG8=","encoding":"base64"}'))

        CodeupApi api = new CodeupApi(null)

        String content = api.getFileContent('codeup.example.com', 'pt-token', 'group/demo', 'src/main.txt', 'master')

        assertEquals('hello', content)
        assertEquals('https://codeup.example.com/oapi/v1/codeup/repositories/group%2Fdemo/files/src%2Fmain.txt?ref=master', captured.url)
    }

    @Test
    void getFileContentReturnsPlainTextWhenEncodingIsText() {
        Map<String, Object> captured = [:]
        stubGetRequest(captured, new HttpUtils.HttpResponse(HttpURLConnection.HTTP_OK, '{"content":"hello","encoding":"text"}'))

        CodeupApi api = new CodeupApi(null)

        String content = api.getFileContent('codeup.example.com/', 'pt-token', 'group/demo', 'dir/app config.yml', 'feature/test', 'org-id')

        assertEquals('hello', content)
        assertEquals('https://codeup.example.com/oapi/v1/codeup/organizations/org-id/repositories/group%2Fdemo/files/dir%2Fapp%20config.yml?ref=feature%2Ftest', captured.url)
    }

    @Test
    void fileExistsThrowsWhenResponseIsUnexpected() {
        stubGetRequest([:], new HttpUtils.HttpResponse(HttpURLConnection.HTTP_FORBIDDEN, '{"message":"forbidden"}'))

        CodeupApi api = new CodeupApi(null)

        RuntimeException error = assertThrows(RuntimeException.class) {
            api.fileExists('pt-token', '2813489', 'private.txt', 'master')
        }
        assertTrue(error.message.contains('响应码: 403'))
    }

    @Test
    void getFileContentReturnsNullWhenCodeupReturns404() {
        stubGetRequest([:], new HttpUtils.HttpResponse(HttpURLConnection.HTTP_NOT_FOUND, ''))

        CodeupApi api = new CodeupApi(null)

        assertNull(api.getFileContent('pt-token', '2813489', 'missing.txt', 'master'))
    }

    @Test
    void getFileContentThrowsWhenResponseIsUnexpected() {
        stubGetRequest([:], new HttpUtils.HttpResponse(HttpURLConnection.HTTP_FORBIDDEN, '{"message":"forbidden"}'))

        CodeupApi api = new CodeupApi(null)

        RuntimeException error = assertThrows(RuntimeException.class) {
            api.getFileContent('pt-token', '2813489', 'private.txt', 'master')
        }
        assertTrue(error.message.contains('响应码: 403'))
    }

    @Test
    void getFileContentReturnsNullWhenPayloadHasNoContent() {
        stubGetRequest([:], new HttpUtils.HttpResponse(HttpURLConnection.HTTP_OK, '{"encoding":"base64"}'))

        CodeupApi api = new CodeupApi(null)

        assertNull(api.getFileContent('pt-token', '2813489', 'empty.txt', 'master'))
    }

    @Test
    void listFilesReturnsRegionEditionFileTree() {
        Map<String, Object> captured = [:]
        stubGetRequest(captured, new HttpUtils.HttpResponse(HttpURLConnection.HTTP_OK, buildFilesJson([
                [id: '1', name: 'main.java', path: 'src/main.java', type: 'blob'],
                [id: '2', name: 'test', path: 'src/test', type: 'tree']
        ])))

        CodeupApi api = new CodeupApi(null)

        List<Map<String, Object>> files = api.listFiles('pt-token', 'group/demo', 'src/main test', 'feature/test', 'DIRECT')

        assertEquals(2, files.size())
        assertEquals('main.java', files[0].name)
        assertEquals('blob', files[0].type)
        assertEquals('https://openapi-rdc.aliyuncs.com/oapi/v1/codeup/repositories/group%2Fdemo/files/tree?path=src%2Fmain%20test&ref=feature%2Ftest&type=DIRECT', captured.url)
        assertEquals('pt-token', captured.headers['x-yunxiao-token'])
    }

    @Test
    void listFilesSupportsCenterEditionWhenOrganizationIdProvided() {
        Map<String, Object> captured = [:]
        stubGetRequest(captured, new HttpUtils.HttpResponse(HttpURLConnection.HTTP_OK, buildFilesJson([
                [id: '1', name: 'demo.txt', path: 'src/demo.txt', type: 'blob']
        ])))

        CodeupApi api = new CodeupApi(null)

        List<Map<String, Object>> files = api.listFiles('codeup.example.com', 'pt-token', 'group/demo', 'src', 'master', 'RECURSIVE', 'org-id')

        assertEquals(1, files.size())
        assertEquals('demo.txt', files[0].name)
        assertEquals('https://codeup.example.com/oapi/v1/codeup/organizations/org-id/repositories/group%2Fdemo/files/tree?path=src&ref=master&type=RECURSIVE', captured.url)
    }

    @Test
    void listFilesOmitsBlankQueryParameters() {
        Map<String, Object> captured = [:]
        stubGetRequest(captured, new HttpUtils.HttpResponse(HttpURLConnection.HTTP_OK, buildFilesJson([])))

        CodeupApi api = new CodeupApi(null)

        List<Map<String, Object>> files = api.listFiles('codeup.example.com/', 'pt-token', 'group/demo', '', null, '  ')

        assertNotNull(files)
        assertTrue(files.isEmpty())
        assertEquals('https://codeup.example.com/oapi/v1/codeup/repositories/group%2Fdemo/files/tree', captured.url)
    }

    @Test
    void listFilesThrowsWhenResponseIsUnexpected() {
        stubGetRequest([:], new HttpUtils.HttpResponse(HttpURLConnection.HTTP_FORBIDDEN, '{"message":"forbidden"}'))

        CodeupApi api = new CodeupApi(null)

        RuntimeException error = assertThrows(RuntimeException.class) {
            api.listFiles('pt-token', 'group/demo', 'src', 'master', 'DIRECT')
        }
        assertTrue(error.message.contains('响应码: 403'))
    }

    @Test
    void listRepositoriesRequiresOrganizationIdWhenUsingDefaultDomain() {
        CodeupApi api = new CodeupApi(null)

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class) {
            api.listRepositories('pt-token')
        }

        assertEquals('organizationId空的', error.message)
    }

    @Test
    void listRepositoriesAggregatesAllPages() {
        List<Map<String, Object>> capturedRequests = []
        stubSequentialGetRequests(capturedRequests, [
                new HttpUtils.HttpResponse(HttpURLConnection.HTTP_OK, buildRepositoriesJson(100, 1)),
                new HttpUtils.HttpResponse(HttpURLConnection.HTTP_OK, buildRepositoriesJson(1, 101))
        ])

        CodeupApi api = new CodeupApi(null)

        List<Map<String, Object>> repositories = api.listRepositories('pt-token', 'org-id')

        assertEquals(101, repositories.size())
        assertEquals('repo-1', repositories.first().name)
        assertEquals('repo-101', repositories.last().name)
        assertEquals(2, capturedRequests.size())
        assertEquals('https://openapi-rdc.aliyuncs.com/oapi/v1/codeup/organizations/org-id/repositories?page=1&perPage=100', capturedRequests[0].url)
        assertEquals('https://openapi-rdc.aliyuncs.com/oapi/v1/codeup/organizations/org-id/repositories?page=2&perPage=100', capturedRequests[1].url)
        assertEquals('pt-token', capturedRequests[0].headers['x-yunxiao-token'])
        assertEquals('pt-token', capturedRequests[1].headers['x-yunxiao-token'])
    }

    @Test
    void listRepositoriesSupportsCustomDomainWhenProvided() {
        Map<String, Object> captured = [:]
        stubGetRequest(captured, new HttpUtils.HttpResponse(HttpURLConnection.HTTP_OK, buildRepositoriesJson(1, 1)))

        CodeupApi api = new CodeupApi(null)

        List<Map<String, Object>> repositories = api.listRepositories('codeup.example.com', 'pt-token', 'org-id')

        assertEquals(1, repositories.size())
        assertEquals('https://codeup.example.com/oapi/v1/codeup/organizations/org-id/repositories?page=1&perPage=100', captured.url)
    }

    @Test
    void listRepositoriesRequestsNextPageWhenCurrentPageHasExactlyPageSizeItems() {
        List<Map<String, Object>> capturedRequests = []
        stubSequentialGetRequests(capturedRequests, [
                new HttpUtils.HttpResponse(HttpURLConnection.HTTP_OK, buildRepositoriesJson(100, 1)),
                new HttpUtils.HttpResponse(HttpURLConnection.HTTP_OK, '[]')
        ])

        CodeupApi api = new CodeupApi(null)

        List<Map<String, Object>> repositories = api.listRepositories('pt-token', 'org-id')

        assertEquals(100, repositories.size())
        assertEquals(2, capturedRequests.size())
        assertEquals('https://openapi-rdc.aliyuncs.com/oapi/v1/codeup/organizations/org-id/repositories?page=2&perPage=100', capturedRequests[1].url)
    }

    @Test
    void listRepositoriesReturnsEmptyListWhenApiReturnsEmptyArray() {
        stubGetRequest([:], new HttpUtils.HttpResponse(HttpURLConnection.HTTP_OK, '[]'))

        CodeupApi api = new CodeupApi(null)

        List<Map<String, Object>> repositories = api.listRepositories('pt-token', 'org-id')

        assertNotNull(repositories)
        assertTrue(repositories.isEmpty())
    }

    @Test
    void listRepositoriesSupportsCustomDomainWhenOrganizationIdProvided() {
        Map<String, Object> captured = [:]
        stubGetRequest(captured, new HttpUtils.HttpResponse(HttpURLConnection.HTTP_OK, buildRepositoriesJson(1, 1)))

        CodeupApi api = new CodeupApi(null)

        List<Map<String, Object>> repositories = api.listRepositories('codeup.example.com', 'pt-token', 'org-id')

        assertEquals(1, repositories.size())
        assertEquals('https://codeup.example.com/oapi/v1/codeup/organizations/org-id/repositories?page=1&perPage=100', captured.url)
    }

    @Test
    void listRepositoriesThrowsWhenResponseIsUnexpected() {
        stubGetRequest([:], new HttpUtils.HttpResponse(HttpURLConnection.HTTP_FORBIDDEN, '{"message":"forbidden"}'))

        CodeupApi api = new CodeupApi(null)

        RuntimeException error = assertThrows(RuntimeException.class) {
            api.listRepositories('codeup.example.com', 'pt-token', 'org-id')
        }
        assertTrue(error.message.contains('响应码: 403'))
        assertTrue(error.message.contains('page: 1'))
        assertTrue(error.message.contains('https://codeup.example.com'))
    }

    private static void stubGetRequest(Map<String, Object> captured, HttpUtils.HttpResponse response) {
        ExpandoMetaClass emc = new ExpandoMetaClass(HttpUtils.HttpRequest, false, true)
        emc.'static'.get = { String url ->
            captured.url = url
            return new FakeHttpRequest(captured, response)
        }
        emc.initialize()
        GroovySystem.metaClassRegistry.setMetaClass(HttpUtils.HttpRequest, emc)
    }

    private static void stubSequentialGetRequests(List<Map<String, Object>> capturedRequests, List<HttpUtils.HttpResponse> responses) {
        ExpandoMetaClass emc = new ExpandoMetaClass(HttpUtils.HttpRequest, false, true)
        int index = 0
        emc.'static'.get = { String url ->
            Map<String, Object> captured = [url: url, headers: [:]]
            capturedRequests.add(captured)
            HttpUtils.HttpResponse response = responses[index]
            index++
            return new FakeHttpRequest(captured, response)
        }
        emc.initialize()
        GroovySystem.metaClassRegistry.setMetaClass(HttpUtils.HttpRequest, emc)
    }

    private static String buildRepositoriesJson(int count, int startIndex) {
        List<Map<String, Object>> repositories = (0..<count).collect { int offset ->
            int id = startIndex + offset
            return [
                    id                : id,
                    name              : "repo-${id}",
                    path              : "repo-${id}",
                    nameWithNamespace : "org / repo-${id}",
                    pathWithNamespace : "org/repo-${id}",
                    webUrl            : "https://codeup.example.com/org/repo-${id}",
                    archived          : false,
                    visibility        : 'private'
            ]
        }
        return JsonUtils.toJsonStr(repositories)
    }

    private static String buildFilesJson(List<Map<String, Object>> files) {
        return JsonUtils.toJsonStr(files)
    }

    static class FakeHttpRequest {
        private final Map<String, Object> captured
        private final HttpUtils.HttpResponse response

        FakeHttpRequest(Map<String, Object> captured, HttpUtils.HttpResponse response) {
            this.captured = captured
            this.response = response
            this.captured.headers = [:]
        }

        FakeHttpRequest header(String name, String value) {
            captured.headers[name] = value
            return this
        }

        FakeHttpRequest timeout(int timeout) {
            captured.timeout = timeout
            return this
        }

        HttpUtils.HttpResponse execute() {
            return response
        }
    }
}
