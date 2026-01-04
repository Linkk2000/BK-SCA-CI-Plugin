package com.tencent.bk.devops.plugin.pojo.kubernetes

data class CreateServiceReq(
    val serviceName: String,
    val portMapping: List<PortMapping>
)

data class PortMapping(
    val name: String,
    val port: Int
)
