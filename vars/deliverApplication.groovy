import com.daluobai.jenkinslib.delivery.ApplicationDeliveryService
import com.daluobai.jenkinslib.delivery.DeliveryRuntime
import com.daluobai.jenkinslib.delivery.DeliveryRuntimeFactory

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
        return new ApplicationDeliveryService(runtime).deliver(request ?: [:])
    }
}

/** 测试 Jenkins 与本地契约测试使用的显式 runtime seam。 */
def call(Map request, DeliveryRuntime runtime) {
    if (request?.legacyCompatibility == true || request?.legacyNoop == true) {
        throw new IllegalArgumentException('替代入口不接受旧入口兼容标志')
    }
    return new ApplicationDeliveryService(runtime).deliver(request ?: [:])
}
