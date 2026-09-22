package com.tencent.bk.devops.atom.task;

import com.tencent.bk.devops.atom.common.Status;
import com.tencent.bk.devops.atom.pojo.AtomResult;
import com.tencent.bk.devops.atom.task.pojo.XMirrorScaAtomParam;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class XMirrorScaAtomTest {

    private MockWebServer server;
    private XMirrorScaAtom atom;

    @Before
    public void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        atom = new XMirrorScaAtom();
    }

    @After
    public void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    public void gateDisabledReturnsAfterStartingScan() throws Exception {
        enqueueJson(detectResponse(101, 202));
        AtomResult result = execute(param("off", null, null));

        assertEquals(Status.success, result.getStatus());
        assertEquals("扫描任务已成功启动", result.getMessage());
        RecordedRequest detect = takeRequest();
        assertEquals("/sca/api-v1/common/task/batch/detect", detect.getPath());
        assertEquals("user-token", detect.getHeader("OpenApiUserToken"));
        assertEquals("{\"ids\":[42]}", detect.getBody().readUtf8());
        assertNull(server.takeRequest(100, TimeUnit.MILLISECONDS));
    }

    @Test
    public void gatePassesAfterFinishedScan() throws Exception {
        enqueueJson(detectResponse(101, 202));
        enqueueJson("{\"code\":0,\"data\":5}");
        enqueueJson("{\"code\":0,\"data\":{\"block\":false}}");

        AtomResult result = execute(param("on", "engine-token-value", "off"));

        assertEquals(Status.success, result.getStatus());
        assertTrue(result.getMessage().contains("质量门禁通过"));
        takeRequest();
        RecordedRequest status = takeRequest();
        assertEquals("/sca/api-v1/open-api-v1/task/status/202", status.getPath());
        RecordedRequest gate = takeRequest();
        assertEquals("/sca/api-v1/engine/rule/handle/rule", gate.getPath());
        assertEquals("engine-token-value", gate.getHeader("engine-token"));
        assertEquals("{\"sceneCode\":1,\"projectId\":101,\"taskId\":202}", gate.getBody().readUtf8());
    }

    @Test
    public void blockingStrategyAlwaysFails() throws Exception {
        enqueueJson(detectResponse(101, 202));
        enqueueJson("{\"code\":0,\"data\":5}");
        enqueueJson("{\"code\":0,\"data\":{\"block\":true,\"strategyLogId\":303}}");

        AtomResult result = execute(param("on", "engine-token-value", "on"));

        assertEquals(Status.failure, result.getStatus());
        assertTrue(result.getMessage().contains("命中阻断策略"));
        assertTrue(result.getMessage().contains("阻断记录ID=303"));
    }

    @Test
    public void gateFailureHonorsFailOpenAndFailClosed() throws Exception {
        enqueueJson(detectResponse(101, 202));
        enqueueJson("{\"code\":0,\"data\":5}");
        enqueueJson("{\"code\":500,\"message\":\"gate unavailable\"}");
        AtomResult open = execute(param("on", "engine-token-value", "on"));
        assertEquals(Status.success, open.getStatus());
        assertTrue(open.getMessage().contains("门禁故障已放行"));

        enqueueJson(detectResponse(101, 202));
        enqueueJson("{\"code\":0,\"data\":5}");
        enqueueJson("{\"code\":500,\"message\":\"gate unavailable\"}");
        AtomResult closed = execute(param("on", "engine-token-value", "off"));
        assertEquals(Status.failure, closed.getStatus());
        assertTrue(closed.getMessage().contains("门禁故障已阻断"));
    }

    @Test
    public void gateRequiresEngineTokenBeforeNetworkCall() throws Exception {
        AtomResult result = execute(param("on", "  ", "off"));

        assertEquals(Status.failure, result.getStatus());
        assertTrue(result.getMessage().contains("未配置引擎令牌"));
        assertNull(server.takeRequest(100, TimeUnit.MILLISECONDS));
    }

    @Test
    public void gateFallsBackToStoreSensitiveEngineToken() throws Exception {
        enqueueJson(detectResponse(101, 202));
        enqueueJson("{\"code\":0,\"data\":5}");
        enqueueJson("{\"code\":0,\"data\":{\"block\":false}}");
        XMirrorScaAtomParam param = param("on", null, "off");
        param.setBkSensitiveConfInfo(Collections.singletonMap("engineToken", "store-token"));

        AtomResult result = execute(param);

        assertEquals(Status.success, result.getStatus());
        takeRequest();
        takeRequest();
        assertEquals("store-token", takeRequest().getHeader("engine-token"));
    }

    @Test
    public void pipelineEngineTokenTakesPrecedenceOverStoreSensitiveToken() throws Exception {
        enqueueJson(detectResponse(101, 202));
        enqueueJson("{\"code\":0,\"data\":5}");
        enqueueJson("{\"code\":0,\"data\":{\"block\":false}}");
        XMirrorScaAtomParam param = param("on", "pipeline-token", "off");
        param.setBkSensitiveConfInfo(Collections.singletonMap("engineToken", "store-token"));

        AtomResult result = execute(param);

        assertEquals(Status.success, result.getStatus());
        takeRequest();
        takeRequest();
        assertEquals("pipeline-token", takeRequest().getHeader("engine-token"));
    }

    @Test
    public void blankStoreSensitiveEngineTokenStillFailsBeforeNetworkCall() throws Exception {
        XMirrorScaAtomParam param = param("on", null, "off");
        param.setBkSensitiveConfInfo(Collections.singletonMap("engineToken", " "));

        AtomResult result = execute(param);

        assertEquals(Status.failure, result.getStatus());
        assertTrue(result.getMessage().contains("未配置引擎令牌"));
        assertNull(server.takeRequest(100, TimeUnit.MILLISECONDS));
    }

    @Test
    public void terminalScanStatusUsesConfiguredFailurePolicy() throws Exception {
        enqueueJson(detectResponse(101, 202));
        enqueueJson("{\"code\":0,\"data\":8}");

        AtomResult result = execute(param("on", "engine-token-value", "off"));

        assertEquals(Status.failure, result.getStatus());
        assertTrue(result.getMessage().contains("扫描未正常完成，状态=8"));
    }

    private AtomResult execute(XMirrorScaAtomParam param) {
        AtomResult result = new AtomResult();
        atom.execute(param, result);
        return result;
    }

    private XMirrorScaAtomParam param(String qualityEnable, String engineToken, String failOpen) {
        XMirrorScaAtomParam param = new XMirrorScaAtomParam();
        param.setServer(server.url("/").toString());
        param.setToken("user-token");
        param.setApplicationId("42");
        param.setQualityEnable(qualityEnable);
        param.setEngineToken(engineToken);
        param.setFailOpen(failOpen);
        param.setTimeoutMinutes(1);
        return param;
    }

    private void enqueueJson(String body) {
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody(body));
    }

    private RecordedRequest takeRequest() throws InterruptedException {
        RecordedRequest request = server.takeRequest(1, TimeUnit.SECONDS);
        assertNotNull(request);
        return request;
    }

    private static String detectResponse(int projectId, int taskId) {
        return "{\"code\":0,\"data\":{\"detectReturnVOList\":[{\"projectId\":"
                + projectId + ",\"scaTaskId\":" + taskId + "}]}}";
    }
}
