package com.daluobai.jenkinslib.delivery

import com.daluobai.jenkinslib.utils.MapUtils

class EffectiveDeliveryConfigResolver implements Serializable {
    static final List<String> LAYERS = ['defaults', 'extension', 'primary', 'overrides']

    Map resolve(Map request) {
        if (!(request.primary instanceof Map)) {
            throw new IllegalArgumentException('primary统一交付配置为空')
        }
        List<Map> layers = LAYERS.collect { String name ->
            Object layer = request[name]
            if (layer != null && !(layer instanceof Map)) {
                throw new IllegalArgumentException("${name}配置必须是Map")
            }
            Map normalized = MapUtils.deepCopy((layer ?: [:]) as Map)
            if (normalized.containsKey('DEPLOY_PIPELINE')) {
                throw new IllegalArgumentException("替代入口不接受旧配置结构: ${name}.DEPLOY_PIPELINE")
            }
            return normalized
        }
        Map effective = MapUtils.deepCopy(MapUtils.merge(layers) as Map)
        return [effectiveConfig: effective, configurationSources: new ArrayList<>(LAYERS)]
    }
}
