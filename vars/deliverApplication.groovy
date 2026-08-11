import com.daluobai.jenkinslib.delivery.ApplicationDeliveryService
import com.daluobai.jenkinslib.delivery.DeliveryRuntime
import com.daluobai.jenkinslib.delivery.DeliveryRuntimeFactory
import com.daluobai.jenkinslib.constant.EFileReadType
import com.daluobai.jenkinslib.utils.ConfigUtils
import com.daluobai.jenkinslib.utils.MapUtils
import groovy.transform.Field

@Field
static final String SHARED_LIBRARY_DEFAULTS_PATH = 'config/delivery-defaults.json'

/**
 * 当前统一应用交付入口。只接受 defaults、extension、primary、overrides 四层统一 DELIVERY 配置。
 */
def call(Map request) {
    if (request?.legacyCompatibility == true || request?.legacyNoop == true) {
        throw new IllegalArgumentException('替代入口不接受旧入口兼容标志')
    }
    return node('buildNode') {
        DeliveryRuntime runtime = binding.hasVariable('deliveryRuntime') && binding.getVariable('deliveryRuntime') instanceof DeliveryRuntime
                ? binding.getVariable('deliveryRuntime') as DeliveryRuntime
                : DeliveryRuntimeFactory.forJenkins(this)
        return new ApplicationDeliveryService(runtime).deliver(withSharedLibraryDefaults(request ?: [:]))
    }
}

/** 测试 Jenkins 与本地契约测试使用的显式 runtime seam。 */
def call(Map request, DeliveryRuntime runtime) {
    if (request?.legacyCompatibility == true || request?.legacyNoop == true) {
        throw new IllegalArgumentException('替代入口不接受旧入口兼容标志')
    }
    return new ApplicationDeliveryService(runtime).deliver(withSharedLibraryDefaults(request ?: [:]))
}

private Map withSharedLibraryDefaults(Map request) {
    Object rawDefaults = new ConfigUtils(this).readConfig(EFileReadType.RESOURCES, SHARED_LIBRARY_DEFAULTS_PATH)
    if (!(rawDefaults instanceof Map)) {
        throw new IllegalArgumentException('共享库默认配置根节点必须是Map')
    }
    if (request.defaults != null && !(request.defaults instanceof Map)) {
        throw new IllegalArgumentException('defaults配置必须是Map')
    }
    Map prepared = MapUtils.deepCopy(request ?: [:]) as Map
    prepared.sharedLibraryDefaults = MapUtils.deepCopy(rawDefaults as Map)
    Object rawProvenance = prepared.provenance
    if (rawProvenance != null && !(rawProvenance instanceof Map)) {
        throw new IllegalArgumentException('provenance配置必须是Map')
    }
    Map provenance = MapUtils.deepCopy((rawProvenance ?: [:]) as Map)
    provenance.sharedLibraryDefaults = SHARED_LIBRARY_DEFAULTS_PATH
    prepared.provenance = provenance
    return prepared
}
