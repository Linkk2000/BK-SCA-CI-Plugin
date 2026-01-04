package com.tencent.bk.devops.plugin.docker.pojo

import com.tencent.bk.devops.plugin.pojo.docker.DockerRunResponse

data class DockerRunResponse(
    val extraOptions: Map<String, String>,
    val containerId: String? = "",
    val startTimeStamp: Int? = 0,
    val dockerRunPortBindings: List<DockerRunPortBinding>? = emptyList(),
) {

    data class DockerRunPortBinding(
        val hostIp: String,
        /**
         *  容器Port
         */
        val containerPort: Int,
        /**
         * 构建机Port
         */
        val hostPort: Int,
    )
    override fun toString(): String {
        return "${extraOptions.filter { !it.key.contains("token", ignoreCase = true) }}"
    }
}
