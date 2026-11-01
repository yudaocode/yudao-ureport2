package cn.iocoder.ureport2.compat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.annotation.DirtiesContext;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 非空 contextPath 下，三个页面按 contextPath 加载 ureport-token.js，静态资源可访问
 *
 * @author 芋道源码
 */
@SpringBootTest(classes = CompatApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "server.servlet.context-path=/ctx")
// 每个测试类独立 Web 上下文：类结束后关闭，避免多个缓存上下文与 UReport 静态状态互相干扰
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class UReportTokenContextPathTest {

    private static final String CTX = "/ctx";

    @Autowired
    private Environment environment;

    private int port() {
        return environment.getRequiredProperty("local.server.port", Integer.class);
    }

    @Test
    void pagesLoadHelperUnderContextPath() throws Exception {
        HttpTestClient.Response designer = HttpTestClient.get(port(), CTX + "/ureport/designer?token=abc");
        assertEquals(200, designer.status, designer.describe());
        UReportTokenPassThroughTest.assertPageLoadsHelper(designer.text(), CTX, "designer.bundle.js");
        assertTrue(designer.text().contains("window._server=\"" + CTX + "/ureport\""), designer.text());

        HttpTestClient.Response searchForm = HttpTestClient.get(port(), CTX + "/ureport/searchFormDesigner?token=abc");
        assertEquals(200, searchForm.status, searchForm.describe());
        UReportTokenPassThroughTest.assertPageLoadsHelper(searchForm.text(), CTX, "searchform.bundle.js");

        HttpTestClient.Response preview = HttpTestClient.get(port(), CTX + "/ureport/preview?_u=mem:demo.ureport.xml&token=abc");
        assertEquals(200, preview.status, preview.describe());
        UReportTokenPassThroughTest.assertPageLoadsHelper(preview.text(), CTX, "preview.bundle.js");

        HttpTestClient.Response js = HttpTestClient.get(port(), CTX + UReportTokenPassThroughTest.HELPER);
        assertEquals(200, js.status, js.describe());
    }

}
