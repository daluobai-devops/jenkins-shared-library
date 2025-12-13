package com.daluobai.jenkinslib.utils

/**
 * @author daluobai@outlook.com
 * version 1.0.0
 * @title HTTP工具类 - 参考hutool HttpUtil
 * @description https://github.com/daluobai-devops/jenkins-shared-library
 * @create 2025/12/13
 */
class HttpUtils implements Serializable {

    /**
     * 发送GET请求
     * @param url 请求URL
     * @param timeout 超时时间（毫秒）
     * @return 响应内容
     */
    static String get(String url, int timeout = 30000) {
        AssertUtils.notBlank(url, "URL不能为空")
        
        try {
            URL urlObject = new URL(url)
            HttpURLConnection connection = (HttpURLConnection) urlObject.openConnection()
            
            connection.setRequestMethod("GET")
            connection.setConnectTimeout(timeout)
            connection.setReadTimeout(timeout)
            connection.setRequestProperty("User-Agent", "Jenkins-Shared-Library/1.0")
            
            int responseCode = connection.getResponseCode()
            if (responseCode == HttpURLConnection.HTTP_OK) {
                return connection.getInputStream().getText("UTF-8")
            } else {
                throw new RuntimeException("HTTP请求失败，响应码: " + responseCode)
            }
        } catch (Exception e) {
            throw new RuntimeException("GET请求失败: " + url, e)
        }
    }

    /**
     * 发送GET请求（带参数）
     * @param url 请求URL
     * @param params 参数Map
     * @param timeout 超时时间（毫秒）
     * @return 响应内容
     */
    static String get(String url, Map<String, String> params, int timeout = 30000) {
        AssertUtils.notBlank(url, "URL不能为空")
        
        if (params != null && !params.isEmpty()) {
            StringBuilder urlBuilder = new StringBuilder(url)
            if (!url.contains("?")) {
                urlBuilder.append("?")
            } else if (!url.endsWith("&")) {
                urlBuilder.append("&")
            }
            
            params.each { key, value ->
                urlBuilder.append(URLEncoder.encode(key, "UTF-8"))
                        .append("=")
                        .append(URLEncoder.encode(value ?: "", "UTF-8"))
                        .append("&")
            }
            
            url = urlBuilder.substring(0, urlBuilder.length() - 1)
        }
        
        return get(url, timeout)
    }

    /**
     * 发送POST请求（表单）
     * @param url 请求URL
     * @param params 参数Map
     * @param timeout 超时时间（毫秒）
     * @return 响应内容
     */
    static String post(String url, Map<String, String> params = [:], int timeout = 30000) {
        AssertUtils.notBlank(url, "URL不能为空")
        
        try {
            URL urlObject = new URL(url)
            HttpURLConnection connection = (HttpURLConnection) urlObject.openConnection()
            
            connection.setRequestMethod("POST")
            connection.setConnectTimeout(timeout)
            connection.setReadTimeout(timeout)
            connection.setDoOutput(true)
            connection.setRequestProperty("User-Agent", "Jenkins-Shared-Library/1.0")
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            
            if (params != null && !params.isEmpty()) {
                StringBuilder postData = new StringBuilder()
                params.each { key, value ->
                    if (postData.length() > 0) {
                        postData.append("&")
                    }
                    postData.append(URLEncoder.encode(key, "UTF-8"))
                            .append("=")
                            .append(URLEncoder.encode(value ?: "", "UTF-8"))
                }
                
                connection.getOutputStream().write(postData.toString().getBytes("UTF-8"))
            }
            
            int responseCode = connection.getResponseCode()
            if (responseCode == HttpURLConnection.HTTP_OK) {
                return connection.getInputStream().getText("UTF-8")
            } else {
                throw new RuntimeException("HTTP请求失败，响应码: " + responseCode)
            }
        } catch (Exception e) {
            throw new RuntimeException("POST请求失败: " + url, e)
        }
    }

    /**
     * 发送POST请求（JSON）
     * @param url 请求URL
     * @param jsonBody JSON字符串
     * @param timeout 超时时间（毫秒）
     * @return 响应内容
     */
    static String postJson(String url, String jsonBody, int timeout = 30000) {
        AssertUtils.notBlank(url, "URL不能为空")
        
        try {
            URL urlObject = new URL(url)
            HttpURLConnection connection = (HttpURLConnection) urlObject.openConnection()
            
            connection.setRequestMethod("POST")
            connection.setConnectTimeout(timeout)
            connection.setReadTimeout(timeout)
            connection.setDoOutput(true)
            connection.setRequestProperty("User-Agent", "Jenkins-Shared-Library/1.0")
            connection.setRequestProperty("Content-Type", "application/json;charset=utf-8")
            
            if (StrUtils.isNotBlank(jsonBody)) {
                connection.getOutputStream().write(jsonBody.getBytes("UTF-8"))
            }
            
            int responseCode = connection.getResponseCode()
            if (responseCode == HttpURLConnection.HTTP_OK) {
                return connection.getInputStream().getText("UTF-8")
            } else {
                throw new RuntimeException("HTTP请求失败，响应码: " + responseCode)
            }
        } catch (Exception e) {
            throw new RuntimeException("POST JSON请求失败: " + url, e)
        }
    }

    /**
     * 下载文件
     * @param url 文件URL
     * @param destFile 目标文件
     * @param timeout 超时时间（毫秒）
     */
    static void downloadFile(String url, File destFile, int timeout = 60000) {
        AssertUtils.notBlank(url, "URL不能为空")
        AssertUtils.notNull(destFile, "目标文件不能为null")
        
        try {
            // 确保父目录存在
            File parent = destFile.getParentFile()
            if (parent != null && !parent.exists()) {
                parent.mkdirs()
            }
            
            URL urlObject = new URL(url)
            HttpURLConnection connection = (HttpURLConnection) urlObject.openConnection()
            
            connection.setRequestMethod("GET")
            connection.setConnectTimeout(timeout)
            connection.setReadTimeout(timeout)
            connection.setRequestProperty("User-Agent", "Jenkins-Shared-Library/1.0")
            
            int responseCode = connection.getResponseCode()
            if (responseCode == HttpURLConnection.HTTP_OK) {
                destFile.withOutputStream { os ->
                    connection.getInputStream().withStream { is ->
                        os << is
                    }
                }
            } else {
                throw new RuntimeException("下载文件失败，响应码: " + responseCode)
            }
        } catch (Exception e) {
            throw new RuntimeException("下载文件失败: " + url, e)
        }
    }

    /**
     * HttpRequest类 - 链式调用
     */
    static class HttpRequest {
        private String url
        private String method = "GET"
        private Map<String, String> headers = [:]
        private String body
        private int timeout = 30000

        HttpRequest(String url) {
            this.url = url
        }

        static HttpRequest post(String url) {
            HttpRequest request = new HttpRequest(url)
            request.method = "POST"
            return request
        }

        static HttpRequest get(String url) {
            return new HttpRequest(url)
        }

        HttpRequest contentType(String contentType) {
            headers.put("Content-Type", contentType)
            return this
        }

        HttpRequest header(String name, String value) {
            headers.put(name, value)
            return this
        }

        HttpRequest body(String body) {
            this.body = body
            return this
        }

        HttpRequest timeout(int timeout) {
            this.timeout = timeout
            return this
        }

        HttpResponse execute() {
            try {
                URL urlObject = new URL(url)
                HttpURLConnection connection = (HttpURLConnection) urlObject.openConnection()
                
                connection.setRequestMethod(method)
                connection.setConnectTimeout(timeout)
                connection.setReadTimeout(timeout)
                connection.setRequestProperty("User-Agent", "Jenkins-Shared-Library/1.0")
                
                // 设置请求头
                headers.each { key, value ->
                    connection.setRequestProperty(key, value)
                }
                
                // 写入请求体
                if (StrUtils.isNotBlank(body)) {
                    connection.setDoOutput(true)
                    connection.getOutputStream().write(body.getBytes("UTF-8"))
                }
                
                int responseCode = connection.getResponseCode()
                String responseBody = ""
                
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    responseBody = connection.getInputStream().getText("UTF-8")
                } else {
                    // 尝试读取错误响应
                    try {
                        responseBody = connection.getErrorStream()?.getText("UTF-8") ?: ""
                    } catch (Exception ignored) {
                    }
                }
                
                return new HttpResponse(responseCode, responseBody)
            } catch (Exception e) {
                throw new RuntimeException("HTTP请求失败: " + url, e)
            }
        }
    }

    /**
     * HttpResponse类
     */
    static class HttpResponse {
        private int status
        private String body

        HttpResponse(int status, String body) {
            this.status = status
            this.body = body
        }

        int getStatus() {
            return status
        }

        String body() {
            return body
        }

        boolean isOk() {
            return status == HttpURLConnection.HTTP_OK
        }
    }
}
