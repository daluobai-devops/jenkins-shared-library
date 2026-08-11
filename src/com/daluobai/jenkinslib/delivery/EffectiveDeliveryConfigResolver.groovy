package com.daluobai.jenkinslib.delivery

import com.daluobai.jenkinslib.utils.MapUtils

class EffectiveDeliveryConfigResolver implements Serializable {
    static final List<String> LAYERS = ['defaults', 'extension', 'primary', 'overrides']

    Map resolve(Map request) {
        if (!(request.primary instanceof Map)) {
            throw new IllegalArgumentException('primary统一交付配置为空')
        }
        if (request.sharedLibraryDefaults != null && !(request.sharedLibraryDefaults instanceof Map)) {
            throw new IllegalArgumentException('共享库默认配置必须是Map')
        }
        boolean legacyCompatibility = request.legacyCompatibility == true
        List<Map> layers = LAYERS.collect { String name ->
            Object layer = request[name]
            if (layer != null && !(layer instanceof Map)) {
                throw new IllegalArgumentException("${name}配置必须是Map")
            }
            Map normalized = MapUtils.deepCopy((layer ?: [:]) as Map)
            if (normalized.containsKey('DEPLOY_PIPELINE')) {
                throw new IllegalArgumentException("替代入口不接受旧配置结构: ${name}.DEPLOY_PIPELINE")
            }
            return legacyCompatibility ? normalized : inheritEmptyCredentialAliases(normalized)
        }
        Map sharedLibraryDefaults = MapUtils.deepCopy((request.sharedLibraryDefaults ?: [:]) as Map) as Map
        if (!legacyCompatibility) {
            sharedLibraryDefaults = inheritEmptyCredentialAliases(sharedLibraryDefaults)
        }
        Map effective = MapUtils.deepCopy(MapUtils.merge([sharedLibraryDefaults] + layers) as Map)
        if (!legacyCompatibility) {
            applyDefaultSourceCredential(effective)
        }
        return [effectiveConfig: effective, configurationSources: new ArrayList<>(LAYERS)]
    }

    private static Map inheritEmptyCredentialAliases(Map config) {
        List<Object> removals = []
        config.each { Object key, Object value ->
            if (key?.toString() == 'credentialsId' && value == null) {
                throw new IllegalArgumentException('credentialsId不能为null')
            }
            if (value instanceof Map) {
                inheritEmptyCredentialAliases(value as Map)
            } else if (key?.toString() == 'credentialsId' && value instanceof CharSequence && value.toString().isEmpty()) {
                removals.add(key)
            }
        }
        removals.each { Object key -> config.remove(key) }
        return config
    }

    private static void applyDefaultSourceCredential(Map effective) {
        Map source = effective.DELIVERY?.source as Map
        if (!source) {
            return
        }
        String repository = source.repository?.toString()
        Object configured = source.credentialsId
        if (configured?.toString() == '-') {
            source.remove('credentialsId')
            clearCredentialMarkers(effective)
            return
        }
        if (configured != null && !configured.toString().isEmpty() && isHttpRepository(repository)) {
            throw new IllegalArgumentException('HTTPS私有仓库认证暂不支持')
        }
        if ((configured == null || configured.toString().isEmpty()) && isSshRepository(repository)) {
            Object shared = effective.DEFAULT_CONFIG?.git?.credentialsId
            if (shared != null && !shared.toString().isEmpty() && shared.toString() != '-') {
                source.credentialsId = shared.toString()
            }
        }
        clearCredentialMarkers(effective)
    }

    private static void clearCredentialMarkers(Map config) {
        List<Object> removals = []
        config.each { Object key, Object value ->
            if (value instanceof Map) {
                clearCredentialMarkers(value as Map)
            } else if (key?.toString() == 'credentialsId' && value?.toString() == '-') {
                removals.add(key)
            }
        }
        removals.each { Object key -> config.remove(key) }
    }

    private static boolean isSshRepository(String repository) {
        return repository?.startsWith('ssh://') || repository ==~ /[^\s\/@:]+@[^\s\/:]+:.+/
    }

    private static boolean isHttpRepository(String repository) {
        String normalized = repository?.toLowerCase(Locale.ROOT)
        return normalized?.startsWith('http://') || normalized?.startsWith('https://')
    }
}
