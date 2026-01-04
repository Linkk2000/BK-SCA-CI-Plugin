package com.tencent.bk.devops.atom.task;

import com.tencent.bk.devops.atom.AtomContext;
import com.tencent.bk.devops.atom.common.Status;
import com.tencent.bk.devops.atom.pojo.AtomResult;
import com.tencent.bk.devops.atom.spi.AtomService;
import com.tencent.bk.devops.atom.spi.TaskAtom;
import com.tencent.bk.devops.atom.task.pojo.XMirrorSastAtomParam;
import com.tencent.bk.devops.atom.utils.http.OkHttpUtils;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

@AtomService(paramClass = XMirrorSastAtomParam.class)
public class XMirrorSastAtom implements TaskAtom<XMirrorSastAtomParam> {
    private final static Logger logger = LoggerFactory.getLogger(XMirrorSastAtom.class);
    
    // 这里的 IP 和路径根据你的需求固定
    private static final String CONNECT_URL = "/sast/api-v1/open-api/system/user/token/connect";

    @Override
    public void execute(AtomContext<XMirrorSastAtomParam> atomContext) {
        XMirrorSastAtomParam param = atomContext.getParam();
        AtomResult result = atomContext.getResult();
        
        String token = param.getToken();
        String server = param.getServer();
        logger.info("Starting connection test...");
        
        if (token == null || token.trim().isEmpty()) {
            fail(result, "Token is empty");
            return;
        }
        if (server==null||server.trim().isEmpty()) {
            fail(result, "Server is empty");
            return;
        }
        if (!server.startsWith("http://") && !server.startsWith("https://")) {
            server = "http://" + server;
        }
        // server 如果最后存在 “/”则移除
        server = server.endsWith("/") ? server.substring(0, server.length() - 1) : server;

        // 构建 URL，Query参数使用token
        String url = server + CONNECT_URL + "?token=" + token;
        
        // 构建请求，Header参数也使用同一个token
        Request request = new Request.Builder()
                .url(url)
                .get()
                .addHeader("Sast-Token", token)
                .addHeader("User-Agent", "bk-pipeline-plugin/1.0")
                .build();

        // 使用 doHttpRaw 获取原始 Response 对象以便读取状态码
        try (Response response = OkHttpUtils.doHttpRaw(request)) {
            if (response == null) {
                fail(result, "Response is null");
                return;
            }

            int code = response.code();
            String body = response.body() != null ? response.body().string() : "";
            
            logger.info("Response Code: {}", code);
            logger.info("Response Body: {}", body);
            
            if (response.isSuccessful()) {
                result.setStatus(Status.success);
                result.setMessage("Connection successful");
            } else {
                result.setStatus(Status.failure);
                result.setMessage("Connection failed with status: " + code);
            }
            
        } catch (IOException e) {
            logger.error("Request failed", e);
            fail(result, "Request failed: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error", e);
            fail(result, "Unexpected error: " + e.getMessage());
        }
    }
    
    private void fail(AtomResult result, String message) {
        result.setStatus(Status.failure);
        result.setMessage(message);
    }
}
