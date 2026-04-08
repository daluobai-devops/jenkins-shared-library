package com.daluobai.jenkinslib.vars

import com.daluobai.jenkinslib.api.CodeupApi
import com.daluobai.jenkinslib.utils.HttpUtils
import groovy.lang.ExpandoMetaClass
import groovy.lang.GroovyShell
import groovy.lang.GroovySystem
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertNotNull
import static org.junit.jupiter.api.Assertions.assertSame
import static org.junit.jupiter.api.Assertions.assertTrue

class CodeupVarTest {

    @AfterEach
    void cleanup() {
        GroovySystem.metaClassRegistry.removeMetaClass(HttpUtils.HttpRequest)
    }

    @Test
    void codeupVarReturnsCodeupApiBoundToScriptContext() {
        GroovyShell shell = new GroovyShell(CodeupApi.class.classLoader)
        Script script = shell.parse(new File('vars/codeup.groovy'))

        Object client = script.call()

        assertTrue(client instanceof CodeupApi)
        assertNotNull(client)
        assertSame(script, ((CodeupApi) client).steps)
    }

    @Test
    void codeupVarUsesDefaultOpenapiDomain() {
        GroovyShell shell = new GroovyShell(CodeupApi.class.classLoader)
        Script script = shell.parse(new File('vars/codeup.groovy'))
        Map<String, Object> captured = [:]
        stubGetRequest(captured, new HttpUtils.HttpResponse(HttpURLConnection.HTTP_OK, '{"content":"dGVzdA==","encoding":"base64"}'))

        CodeupApi client = (CodeupApi) script.call()
        boolean exists = client.fileExists('pt-token', 'group/demo', 'src/main.txt', 'master')

        assertTrue(exists)
        assertEquals('https://openapi-rdc.aliyuncs.com/oapi/v1/codeup/repositories/group%2Fdemo/files/src%2Fmain.txt?ref=master', captured.url)
        assertEquals('pt-token', captured.headers['x-yunxiao-token'])
    }

    @Test
    void codeupVarUsesDefaultDomainForGetFileContent() {
        GroovyShell shell = new GroovyShell(CodeupApi.class.classLoader)
        Script script = shell.parse(new File('vars/codeup.groovy'))
        Map<String, Object> captured = [:]
        stubGetRequest(captured, new HttpUtils.HttpResponse(HttpURLConnection.HTTP_OK, '{"content":"aGVsbG8=","encoding":"base64"}'))

        CodeupApi client = (CodeupApi) script.call()
        String content = client.getFileContent('pt-token', 'group/demo', 'src/main.txt', 'master')

        assertEquals('hello', content)
        assertEquals('https://openapi-rdc.aliyuncs.com/oapi/v1/codeup/repositories/group%2Fdemo/files/src%2Fmain.txt?ref=master', captured.url)
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
