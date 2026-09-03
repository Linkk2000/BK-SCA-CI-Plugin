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
import java.util.List;
import java.util.Map;

/**
 * 触发 SCA 代码仓库扫描，并可选地对同一次扫描做 CI/CD 质量门禁。
 *
 * 链路（qualityEnable=on）：
 *   1. POST /sca/api-v1/common/task/batch/detect      触发重检，取 scaTaskId / projectId
 *   2. GET  /sca/api-v1/open-api-v1/task/status/{id}  轮询到 5（检测完成）
 *   3. POST /sca/api-v1/engine/rule/handle/rule       {sceneCode:1, projectId, taskId} -> block
 *   4. block=true 时再调一次 3 取 strategyLogId 供追溯，然后失败
 *
 * 门禁接口按 (projectId, 场景=CI/CD) 取策略、按 taskId 读结果表，不看任务来源，
 * 所以代码仓库扫描的任务可以直接过 CI/CD 策略；一次扫描同时保留轮次对比与门禁。
 * 同一 taskId 的判定在 SCA 侧落日志后即冻结，重复调用返回同一结论。
 */
@AtomService(paramClass = XMirrorScaAtomParam.class)
public class XMirrorScaAtom implements TaskAtom<XMirrorScaAtomParam> {
    private final static Logger logger = LoggerFactory.getLogger(XMirrorScaAtom.class);

    private static final String EXECUTE_URL = "/sca/api-v1/common/task/batch/detect";
    private static final String TASK_STATUS_URL = "/sca/api-v1/open-api-v1/task/status/";
    private static final String RULE_HANDLE_URL = "/sca/api-v1/engine/rule/handle/rule";

    /** SCA 任务状态：5 检测完成；6 超时 / 7 手动停止 / 8 异常 / 9 已删除 视为终止 */
    private static final int STATUS_FINISHED = 5;
    /** 策略场景：1 = CI/CD 流水线（SCA 只定义了 1 和 2） */
    private static final int SCENE_CICD = 1;

    private static final int DEFAULT_TIMEOUT_MINUTES = 30;
    private static final long POLL_INTERVAL_MS = 15_000L;
    private static final long LOG_ID_RETRY_DELAY_MS = 2_000L;

    @Override
    public void execute(AtomContext<XMirrorScaAtomParam> atomContext) {
        XMirrorScaAtomParam param = atomContext.getParam();
        AtomResult result = atomContext.getResult();

        String server = param.getServer();
        String token = param.getToken();
        String applicationId = param.getApplicationId();

        if (isBlank(server) || isBlank(token) || isBlank(applicationId)) {
            fail(result, "插件配置参数不完整，请检查服务器地址、Token和应用选择");
            return;
        }

        boolean gateEnabled = "on".equalsIgnoreCase(param.getQualityEnable());
        if (gateEnabled && isBlank(param.getEngineToken())) {
            // 门禁开了却没给凭据，明确报错，绝不能静默放行
            fail(result, "已开启质量门禁但未配置引擎令牌(engineToken)，请填写 SCA 配置项 sca.engine_api_token_value 的值");
            return;
        }

        server = normalizeServer(server);

        try {
            // 1. 触发扫描（与旧版本相同的调用，只是这次把返回的 taskId 接住）
            DetectResult detect = triggerDetect(server, token, applicationId);
            if (detect == null) {
                fail(result, "触发扫描失败");
                return;
            }
            if (!gateEnabled) {
                result.setStatus(Status.success);
                result.setMessage("扫描任务已成功启动");
                logger.info("扫描任务已成功启动，未开启质量门禁。taskId={}", detect.taskId);
                return;
            }
            if (detect.taskId == null || detect.projectId == null) {
                gateFailure(result, param, "触发成功但服务器未返回 taskId/projectId，无法执行门禁");
                return;
            }
            logger.info("扫描任务已启动，开始等待完成。taskId={} projectId={}", detect.taskId, detect.projectId);

            // 2. 等扫描完成
            int timeoutMinutes = param.getTimeoutMinutes() == null || param.getTimeoutMinutes() <= 0
                    ? DEFAULT_TIMEOUT_MINUTES : param.getTimeoutMinutes();
            Integer finalStatus = waitForFinish(server, token, detect.taskId, timeoutMinutes);
            if (finalStatus == null) {
                gateFailure(result, param, "等待扫描完成超时(" + timeoutMinutes + " 分钟)，taskId=" + detect.taskId);
                return;
            }
            if (finalStatus != STATUS_FINISHED) {
                gateFailure(result, param, "扫描未正常完成，状态=" + finalStatus + "，taskId=" + detect.taskId);
                return;
            }

            // 3. 门禁判定
            GateResult gate = checkGate(server, param.getEngineToken(), detect.projectId, detect.taskId);
            if (gate == null) {
                gateFailure(result, param, "调用门禁接口失败，taskId=" + detect.taskId);
                return;
            }
            if (!gate.block) {
                result.setStatus(Status.success);
                result.setMessage("扫描完成，质量门禁通过。taskId=" + detect.taskId);
                logger.info("质量门禁通过。taskId={} projectId={}", detect.taskId, detect.projectId);
                return;
            }

            // 4. 命中阻断：首次响应不带 strategyLogId（SCA 异步落日志），稍等后再取一次便于追溯
            Integer logId = gate.strategyLogId;
            if (logId == null) {
                sleep(LOG_ID_RETRY_DELAY_MS);
                GateResult again = checkGate(server, param.getEngineToken(), detect.projectId, detect.taskId);
                if (again != null) {
                    logId = again.strategyLogId;
                }
            }
            String msg = "质量门禁未通过：命中阻断策略。taskId=" + detect.taskId
                    + (logId != null ? "，阻断记录ID=" + logId + "（SCA 策略配置与管理 → 告警及阻断记录）" : "");
            logger.error(msg);
            fail(result, msg);
        } catch (IOException e) {
            gateFailure(result, param, "网络连接异常: " + e.getMessage());
        } catch (Exception e) {
            logger.error("插件执行异常", e);
            gateFailure(result, param, "插件执行异常: " + e.getMessage());
        }
    }

    // ---------- 1. 触发扫描 ----------

    private DetectResult triggerDetect(String server, String token, String applicationId) throws IOException {
        Request request = new Request.Builder()
                .url(server + EXECUTE_URL)
                .post(RequestBody.create("{\"ids\":[" + applicationId + "]}",
                        okhttp3.MediaType.get("application/json; charset=utf-8")))
                .addHeader("OpenApiUserToken", token)
                .addHeader("Content-Type", "application/json")
                .build();

        Map<String, Object> resp = call(request, "触发扫描");
        if (resp == null) {
            return null;
        }
        DetectResult r = new DetectResult();
        // data.detectReturnVOList[0]{projectId, applicationId, scaTaskId}
        Object data = resp.get("data");
        if (data instanceof Map) {
            Object list = ((Map<?, ?>) data).get("detectReturnVOList");
            if (list instanceof List && !((List<?>) list).isEmpty()) {
                Object first = ((List<?>) list).get(0);
                if (first instanceof Map) {
                    r.taskId = asInt(((Map<?, ?>) first).get("scaTaskId"));
                    r.projectId = asInt(((Map<?, ?>) first).get("projectId"));
                }
            }
        }
        return r;
    }

    // ---------- 2. 轮询 ----------

    /** 返回最终状态；超时返回 null。响应形如 {"code":0,"data":5}，data 就是状态整数 */
    private Integer waitForFinish(String server, String token, int taskId, int timeoutMinutes) throws IOException {
        long deadline = System.currentTimeMillis() + timeoutMinutes * 60_000L;
        Request request = new Request.Builder()
                .url(server + TASK_STATUS_URL + taskId)
                .get()
                .addHeader("OpenApiUserToken", token)
                .build();
        while (System.currentTimeMillis() < deadline) {
            Map<String, Object> resp = call(request, "查询任务状态");
            Integer status = resp == null ? null : asInt(resp.get("data"));
            if (status != null) {
                if (status == STATUS_FINISHED) {
                    return status;
                }
                if (status == 6 || status == 7 || status == 8 || status == 9) {
                    return status;
                }
                logger.info("扫描进行中，状态={}，{} 秒后重试。taskId={}", status, POLL_INTERVAL_MS / 1000, taskId);
            } else {
                logger.warn("查询任务状态未得到有效结果，{} 秒后重试。taskId={}", POLL_INTERVAL_MS / 1000, taskId);
            }
            sleep(POLL_INTERVAL_MS);
        }
        return null;
    }

    // ---------- 3. 门禁 ----------

    /** {sceneCode:1, projectId, taskId} -> data{block, strategyLogId}。走 engine-token，不需要用户 token */
    private GateResult checkGate(String server, String engineToken, int projectId, int taskId) throws IOException {
        String body = "{\"sceneCode\":" + SCENE_CICD + ",\"projectId\":" + projectId + ",\"taskId\":" + taskId + "}";
        Request request = new Request.Builder()
                .url(server + RULE_HANDLE_URL)
                .post(RequestBody.create(body, okhttp3.MediaType.get("application/json; charset=utf-8")))
                .addHeader("engine-token", engineToken)
                .addHeader("Content-Type", "application/json")
                .build();
        Map<String, Object> resp = call(request, "质量门禁判定");
        if (resp == null) {
            return null;
        }
        Object data = resp.get("data");
        if (!(data instanceof Map)) {
            logger.error("门禁接口返回缺少 data: {}", resp);
            return null;
        }
        GateResult g = new GateResult();
        g.block = Boolean.TRUE.equals(((Map<?, ?>) data).get("block"));
        g.strategyLogId = asInt(((Map<?, ?>) data).get("strategyLogId"));
        return g;
    }

    // ---------- 公共 ----------

    /** 发请求并校验业务码；失败记日志返回 null。403 单独提示，那是 engine-token 不对 */
    @SuppressWarnings("unchecked")
    private Map<String, Object> call(Request request, String action) throws IOException {
        try (Response response = OkHttpUtils.doHttpRaw(request)) {
            String responseBody = response != null && response.body() != null ? response.body().string() : "";
            int httpCode = response != null ? response.code() : -1;
            logger.info("[{}] HTTP {} {}", action, httpCode, abbreviate(responseBody));
            if (responseBody.isEmpty()) {
                logger.error("[{}] 服务器返回内容为空", action);
                return null;
            }
            Map<String, Object> map = JsonUtil.fromJson(responseBody, Map.class);
            if (map == null) {
                logger.error("[{}] 解析服务器返回数据失败", action);
                return null;
            }
            Object codeObj = map.get("code");
            int bizCode = (codeObj instanceof Number) ? ((Number) codeObj).intValue() : -1;
            if (bizCode != 0) {
                String bizMessage = String.valueOf(map.getOrDefault("message", "服务器未返回错误描述"));
                if (bizCode == 403 || httpCode == 403) {
                    logger.error("[{}] 403 拒绝访问：若为门禁接口，请检查 engineToken 是否等于 SCA 的 sca.engine_api_token_value", action);
                } else if (bizCode == 401) {
                    logger.error("[{}] 认证失败(401): {}", action, bizMessage);
                } else {
                    logger.error("[{}] 业务失败({}): {}", action, bizCode, bizMessage);
                }
                return null;
            }
            return map;
        }
    }

    /** 门禁自身故障时按 failOpen 决定放行还是阻断；默认放行（灰度期），稳定后建议改 off */
    private void gateFailure(AtomResult result, XMirrorScaAtomParam param, String reason) {
        boolean failOpen = param.getFailOpen() == null || "on".equalsIgnoreCase(param.getFailOpen());
        if (failOpen) {
            logger.warn("门禁执行故障，已按 failOpen=on 放行：{}", reason);
            result.setStatus(Status.success);
            result.setMessage("门禁故障已放行：" + reason);
        } else {
            logger.error("门禁执行故障，已按 failOpen=off 阻断：{}", reason);
            fail(result, "门禁故障已阻断：" + reason);
        }
    }

    private void fail(AtomResult result, String message) {
        result.setStatus(Status.failure);
        result.setMessage(message);
    }

    private static String normalizeServer(String server) {
        String s = server.trim();
        if (!s.startsWith("http://") && !s.startsWith("https://")) {
            s = "http://" + s;
        }
        while (s.endsWith("/")) {
            s = s.substring(0, s.length() - 1);
        }
        return s;
    }

    private static Integer asInt(Object o) {
        if (o instanceof Number) {
            return ((Number) o).intValue();
        }
        if (o instanceof String && !((String) o).trim().isEmpty()) {
            try {
                return Integer.parseInt(((String) o).trim());
            } catch (NumberFormatException ignore) {
                return null;
            }
        }
        return null;
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private static String abbreviate(String s) {
        return s.length() > 500 ? s.substring(0, 500) + "..." : s;
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static class DetectResult {
        Integer taskId;
        Integer projectId;
    }

    private static class GateResult {
        boolean block;
        Integer strategyLogId;
    }
}
