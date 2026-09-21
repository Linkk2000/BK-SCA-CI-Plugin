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

    /**
     * 质量门禁开关：on / off。off 时行为与旧版本完全一致（触发扫描即返回）
     */
    private String qualityEnable;
    /**
     * SCA 引擎令牌，即 SCA 配置项 sca.engine_api_token_value（docker/gen.cfg 的
     * vars_must_xmsca_web_engine_token）。/api-v1/engine/** 只认这个头。
     * 仅 qualityEnable=on 时必填
     */
    private String engineToken;
    /**
     * 等待扫描完成的上限（分钟）。代码仓库扫描需 SCA 拉取代码，耗时不可控
     */
    private Integer timeoutMinutes;
    /**
     * 门禁自身故障（SCA 不可达、超时、接口异常）时是否放行：on 放行 / off 阻断。
     * 不影响策略判定为阻断的情况——那永远阻断
     */
    private String failOpen;
}

