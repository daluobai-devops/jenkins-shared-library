import com.daluobai.jenkinslib.codeup.CodeupRepositoryAdapter
import com.daluobai.jenkinslib.codeup.ProductionCodeupRepositoryAdapter
import com.daluobai.jenkinslib.codeup.RepositoryDispatchService
import com.daluobai.jenkinslib.delivery.DeliveryRuntime
import com.daluobai.jenkinslib.delivery.DeliveryRuntimeFactory

/**
 * 稳定的 Codeup 仓库分发入口；授权、发现、读取、预检、执行和汇总由 deep module 负责。
 */
def call(Map config = [:]) {
    CodeupRepositoryAdapter codeup = binding.hasVariable('codeupRepositoryAdapter') &&
            binding.getVariable('codeupRepositoryAdapter') instanceof CodeupRepositoryAdapter
            ? binding.getVariable('codeupRepositoryAdapter') as CodeupRepositoryAdapter
            : new ProductionCodeupRepositoryAdapter(this)
    DeliveryRuntime runtime = binding.hasVariable('deliveryRuntime') && binding.getVariable('deliveryRuntime') instanceof DeliveryRuntime
            ? binding.getVariable('deliveryRuntime') as DeliveryRuntime
            : DeliveryRuntimeFactory.forJenkins(this)
    return new RepositoryDispatchService(this, codeup, runtime).dispatch(config ?: [:])
}

/** 测试 Jenkins 与本地契约测试使用的显式 adapter seam。 */
def call(Map config, CodeupRepositoryAdapter codeup, DeliveryRuntime runtime) {
    return new RepositoryDispatchService(this, codeup, runtime).dispatch(config ?: [:])
}
