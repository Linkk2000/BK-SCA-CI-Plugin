package com.tencent.bk.devops.atom.task.pojo;

import com.tencent.bk.devops.atom.pojo.AtomBaseParam;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class XMirrorSastAtomParam extends AtomBaseParam {
    /**
     * 服务器地址
     */
    private String server;
    /**
     * 访问令牌
     */
    private String token;
}
