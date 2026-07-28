import com.daluobai.jenkinslib.delivery.LegacyApplicationDeliveryEntrypoint

/** 既有 Java 入口：旧配置转换后委托统一应用交付 implementation。 */
def call(Map customConfig = [:]) {
    return new LegacyApplicationDeliveryEntrypoint(this).deliver('JAVA', customConfig ?: [:])
}
