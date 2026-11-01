package cn.iocoder.ureport2.compat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.annotation.DirtiesContext;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/**
 * /ureport/res 静态资源边界：只允许 ureport-asserts 目录，拒绝其他 classpath 资源与路径穿越
 *
 * @author 芋道源码
 */
@SpringBootTest(classes = CompatApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
// 每个测试类独立 Web 上下文：类结束后关闭，避免多个缓存上下文与 UReport 静态状态互相干扰
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class UReportResourceBoundaryTest {

    private static final String MARKER = "UREPORT_BOUNDARY_MARKER_NOT_AN_ASSET";

    @Autowired
    private Environment environment;

    private int port() {
        return environment.getRequiredProperty("local.server.port", Integer.class);
    }

    private void assertRejected(String path, String forbiddenContent) throws Exception {
        HttpTestClient.Response resp = HttpTestClient.get(port(), path);
        assertNotEquals(200, resp.status, path + " should be rejected");
        String body = new String(resp.body, StandardCharsets.UTF_8);
        assertFalse(body.contains(forbiddenContent), path + " leaked resource content");
    }

    @Test
    void designerAssetStillServed() throws Exception {
        HttpTestClient.Response resp = HttpTestClient.get(port(), "/ureport/res/ureport-asserts/js/designer.bundle.js");
        assertEquals(200, resp.status, resp.describe());
        assertTrue(resp.body.length > 10000);
    }

    @Test
    void nonAssetClasspathResourcesRejected() throws Exception {
        assertRejected("/ureport/res/ureport-boundary/marker.txt", MARKER);
        assertRejected("/ureport/res/ureport-core-context.xml", "ureport.exportManager");
        assertRejected("/ureport/res/META-INF/yudao-ureport2/core.properties", "component=core");
    }

    @Test
    void traversalRejected() throws Exception {
        assertRejected("/ureport/res/ureport-asserts/../ureport-boundary/marker.txt", MARKER);
        assertRejected("/ureport/res/ureport-asserts/%2e%2e/ureport-boundary/marker.txt", MARKER);
        assertRejected("/ureport/res/ureport-asserts/..%2fureport-boundary/marker.txt", MARKER);
        assertRejected("/ureport/res/ureport-asserts/%2E%2E%2Fureport-core-context.xml", "ureport.exportManager");
        assertRejected("/ureport/res/ureport-asserts/..%5cureport-core-context.xml", "ureport.exportManager");
        assertRejected("/ureport/res/ureport-asserts//..//ureport-core-context.xml", "ureport.exportManager");
    }

    @Test
    void missingAssetIs404() throws Exception {
        assertEquals(404, HttpTestClient.get(port(), "/ureport/res/ureport-asserts/js/not-exist.js").status);
    }

    @Test
    void guardUnitCases() {
        assertNotNull(com.bstek.ureport.console.res.ResourceLoaderServletActionAccess.check("ureport-asserts/js/designer.bundle.js"));
        assertNull(com.bstek.ureport.console.res.ResourceLoaderServletActionAccess.check("ureport-asserts/../x"));
        assertNull(com.bstek.ureport.console.res.ResourceLoaderServletActionAccess.check("ureport-asserts\\..\\x"));
        assertNull(com.bstek.ureport.console.res.ResourceLoaderServletActionAccess.check("ureport-core-context.xml"));
        assertNull(com.bstek.ureport.console.res.ResourceLoaderServletActionAccess.check("ureport-asserts/%2e%2e/x"));
        assertNull(com.bstek.ureport.console.res.ResourceLoaderServletActionAccess.check(""));
    }

}
