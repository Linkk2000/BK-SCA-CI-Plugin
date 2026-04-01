package com.tencent.bk.devops.atom.task.pojo;

import com.tencent.bk.devops.atom.pojo.AtomBaseParam;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class XMirrorScaAtomParam extends AtomBaseParam {
    /**
     * 服务器地址
     */
    private String server;
    /**
     * 访问令牌
     */
    private String token;
    /**
     * 项目ID
     */
    private String projectId;
    /**
     * 项目名称
     */
    private String projectName;
    /**
     * 应用ID
     */
    private String applicationId;
    /**
     * 应用名称
     */
    private String applicationName;
}

