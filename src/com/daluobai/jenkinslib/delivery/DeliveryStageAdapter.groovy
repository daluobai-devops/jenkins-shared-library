package com.daluobai.jenkinslib.delivery

interface DeliveryStageAdapter extends Serializable {
    boolean nodesAvailable(Collection nodes)

    void initializeWorkspace(Map effectiveConfig)

    Map build(Map effectiveConfig, Map preflight)

    Map store(Map effectiveConfig, Map preflight, Map artifact)

    void deploy(Map effectiveConfig, Map preflight, Map artifact)

    void notify(Map effectiveConfig, String status, Map result)

    void cleanupWorkspace(Map effectiveConfig)
}
