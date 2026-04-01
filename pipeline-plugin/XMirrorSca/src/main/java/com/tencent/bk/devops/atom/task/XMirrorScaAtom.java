package com.tencent.bk.devops.atom.task;

import com.tencent.bk.devops.atom.AtomContext;
import com.tencent.bk.devops.atom.common.Status;
import com.tencent.bk.devops.atom.pojo.AtomResult;
import com.tencent.bk.devops.atom.spi.AtomService;
import com.tencent.bk.devops.atom.spi.TaskAtom;
import com.tencent.bk.devops.atom.task.pojo.XMirrorScaAtomParam;
import com.tencent.bk.devops.atom.utils.http.OkHttpUtils;
import com.tencent.bk.devops.atom.utils.json.JsonUtil;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;

@AtomService(paramClass = XMirrorScaAtomParam.class)
public class XMirrorScaAtom implements TaskAtom<XMirrorScaAtomParam> {
    private final static Logger logger = LoggerFactory.getLogger(XMirrorScaAtom.class);

    private static final String EXECUTE_URL = "/sca/api-v1/common/task/batch/detect";

    @Override
    public void execute(AtomContext<XMirrorScaAtomParam> atomContext) {
        XMirrorScaAtomParam param = atomContext.getParam();
        AtomResult result = atomContext.getResult();

        String server = param.getServer();
        String token = param.getToken();
        String applicationId = param.getApplicationId();

        if (server == null || server.trim().isEmpty() || token == null || token.trim().isEmpty() || applicationId == null || applicationId.trim().isEmpty()) {
            fail(result, "插件配置参数不完整，请检查服务器地址、Token和应用选择");
            return;
        }

        if (!server.startsWith("http://") && !server.startsWith("https://")) {
            server = "http://" + server;
        }
        server = server.endsWith("/") ? server.substring(0, server.length() - 1) : server;

        String url = server + EXECUTE_URL;
        RequestBody body = RequestBody.create(
            "{\"ids\":[" + applicationId + "],\"moduleType\":4}",
            okhttp3.MediaType.get("application/json; charset=utf-8")
        );

        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .addHeader("OpenApiUserToken", token)
                .addHeader("Content-Type", "application/json")
                .build();

        try (Response response = OkHttpUtils.doHttpRaw(request)) {
            String responseBody = response != null && response.body() != null ? response.body().string() : "";
            logger.info("HTTP Code: {}, Response: {}", response != null ? response.code() : "null", responseBody);

            if (responseBody.isEmpty()) {
                fail(result, "服务器返回内容为空");
                return;
            }

            // 使用 SDK 提供的 JsonUtil 解析 JSON，比正则表达式更可靠
            Map<String, Object> responseMap = JsonUtil.fromJson(responseBody, Map.class);
            if (responseMap == null) {
                fail(result, "解析服务器返回数据失败");
                return;
            }

            Object codeObj = responseMap.get("code");
            int bizCode = (codeObj instanceof Number) ? ((Number) codeObj).intValue() : -1;
            String bizMessage = (String) responseMap.getOrDefault("message", "服务器未返回错误描述");


            if (bizCode == 0) {
                result.setStatus(Status.success);
                result.setMessage("扫描任务已成功启动");
                logger.info("扫描任务已成功启动");
            } else if (bizCode == 400) {
                fail(result, "请求参数有误(400): " + bizMessage);
                logger.error("请求参数有误(400): " + bizMessage);
            } else if (bizCode == 401) {
                fail(result, "认证失败(401): " + bizMessage);   
                logger.error("认证失败(401): " + bizMessage);
            } else {
                fail(result, "系统异常(" + bizCode + "): " + bizMessage);
                logger.error("系统异常(" + bizCode + "): " + bizMessage);
            }

        } catch (IOException e) {
            fail(result, "网络连接异常: " + e.getMessage());
        } catch (Exception e) {
            fail(result, "插件执行异常: " + e.getMessage());
        }
    }

    private void fail(AtomResult result, String message) {
        result.setStatus(Status.failure);
        result.setMessage(message);
    }
}

