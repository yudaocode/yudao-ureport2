package cn.iocoder.ureport2.compat;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.test.annotation.DirtiesContext;

import jakarta.servlet.Filter;
import jakarta.servlet.http.HttpServletRequest;
import java.io.ByteArrayOutputStream;
import java.net.URLEncoder;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

/**
 * token / tenantId 透明传递：三个页面加载 ureport-token.js 与 no-referrer；
 * token 不回显进 HTML、不进入报表参数；tenantId 与普通参数仍可用；multipart 导入时宿主 getParameter 不破坏 Part
 *
 * @author 芋道源码
 */
@SpringBootTest(classes = {CompatApplication.class, UReportTokenPassThroughTest.RecordingConfig.class},
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
// 每个测试类独立 Web 上下文：类结束后关闭，避免多个缓存上下文与 UReport 静态状态互相干扰
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class UReportTokenPassThroughTest {

    static final String HELPER = "/ureport/res/ureport-asserts/js/ureport-token.js";
    static final String REFERRER_META = "<meta name=\"referrer\" content=\"no-referrer\">";
    private static final String PARAM_REPORT = "mem:params.ureport.xml";
    /** 含 + / = " < > 空格 中文等需编码字符的复杂 token */
    private static final String TOKEN = "Tk+a/b==\"<x>' y&中";
    private static final String TENANT_ID = "t+1 中";

    /**
     * 仅测试使用：模拟宿主过滤器，记录宿主通过 getParameter 读到的 token / tenantId，不做任何鉴权
     *
     * @author 芋道源码
     */
    @TestConfiguration
    static class RecordingConfig {

        static final Map<String, String> SEEN = new ConcurrentHashMap<>();

        @Bean
        FilterRegistrationBean<Filter> tokenRecordingFilter() {
            Filter filter = (request, response, chain) -> {
                HttpServletRequest req = (HttpServletRequest) request;
                String path = req.getRequestURI().substring(req.getContextPath().length());
                String token = req.getParameter("token");
                String tenantId = req.getParameter("tenantId");
                SEEN.put(path, token + "|" + tenantId);
                chain.doFilter(request, response);
            };
            FilterRegistrationBean<Filter> bean = new FilterRegistrationBean<>(filter);
            bean.addUrlPatterns("/ureport/*");
            return bean;
        }
    }

    @Autowired
    private Environment environment;
    @Autowired
    private MemoryReportProvider memoryReportProvider;

    private int port() {
        return environment.getRequiredProperty("local.server.port", Integer.class);
    }

    private static String enc(String s) throws Exception {
        return URLEncoder.encode(s, "UTF-8");
    }

    private static String credentialQuery() throws Exception {
        return "token=" + enc(TOKEN) + "&tenantId=" + enc(TENANT_ID);
    }

    @BeforeEach
    void saveParamReport() {
        memoryReportProvider.saveReport(PARAM_REPORT, paramReportXml());
        RecordingConfig.SEEN.clear();
    }

    static void assertPageLoadsHelper(String html, String contextPath, String bundle) {
        int head = html.indexOf("<head>");
        assertTrue(head >= 0, html);
        String afterHead = html.substring(head + "<head>".length()).trim();
        assertTrue(afterHead.startsWith(REFERRER_META), "referrer meta must be first in <head>: " + html);
        int helper = html.indexOf("src=\"" + contextPath + HELPER + "\"");
        int bundleIndex = html.indexOf(contextPath + "/ureport/res/ureport-asserts/js/" + bundle);
        assertTrue(helper > 0, "helper script missing: " + html);
        assertTrue(bundleIndex > helper, bundle + " must load after helper");
        int server = html.indexOf("window._server=");
        assertTrue(server > 0, html);
    }

    @Test
    void helperIsServedAsStaticAsset() throws Exception {
        HttpTestClient.Response js = HttpTestClient.get(port(), HELPER);
        assertEquals(200, js.status, js.describe());
        assertTrue(js.contentType.startsWith("text/javascript"), js.contentType);
        String text = js.text();
        assertTrue(text.contains("withAuth") && text.contains("XMLHttpRequest"), "unexpected helper content");
    }

    @Test
    void designerAndSearchFormLoadHelperWithoutEchoingToken() throws Exception {
        HttpTestClient.Response designer = HttpTestClient.get(port(), "/ureport/designer?" + credentialQuery());
        assertEquals(200, designer.status, designer.describe());
        assertPageLoadsHelper(designer.text(), "", "designer.bundle.js");
        assertNoTokenEcho(designer.text());

        HttpTestClient.Response searchForm = HttpTestClient.get(port(), "/ureport/searchFormDesigner?_i0&" + credentialQuery());
        assertEquals(200, searchForm.status, searchForm.describe());
        assertPageLoadsHelper(searchForm.text(), "", "searchform.bundle.js");
        assertNoTokenEcho(searchForm.text());

        assertEquals(TOKEN + "|" + TENANT_ID, RecordingConfig.SEEN.get("/ureport/designer"));
        assertEquals(TOKEN + "|" + TENANT_ID, RecordingConfig.SEEN.get("/ureport/searchFormDesigner"));
    }

    @Test
    void previewKeepsTokenOutOfHtmlAndReportParameters() throws Exception {
        String path = "/ureport/preview?_u=" + enc(PARAM_REPORT) + "&plain=PlainValue42&" + credentialQuery();
        HttpTestClient.Response resp = HttpTestClient.get(port(), path);
        assertEquals(200, resp.status, resp.describe());
        String html = resp.text();
        assertPageLoadsHelper(html, "", "preview.bundle.js");

        // 普通参数与 tenantId 仍进入报表参数；tenantId 只按容器解码一次，+ 不被二次解码成空格
        assertTrue(html.contains("P:PlainValue42"), html);
        // 原 HTML 渲染把空格输出为 &nbsp;，按显示文本比较；+ 与中文仍严格保留
        assertTrue(html.replace("&nbsp;", " ").contains("T:" + TENANT_ID), html);
        // token 不进入报表参数：param('token') 为空
        assertFalse(html.contains("K:Tk"), html);
        assertNoTokenEcho(html);
        // 普通参数仍按原逻辑回显，供分页/查询链接使用
        assertTrue(html.contains("plain=PlainValue42"), html);
        assertFalse(html.contains("tenantId="), "tenantId should not be echoed by server: " + html);

        assertEquals(TOKEN + "|" + TENANT_ID, RecordingConfig.SEEN.get("/ureport/preview"));
    }

    @Test
    void multipartImportReadsQueryWithoutBreakingPart() throws Exception {
        byte[] xlsx;
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            wb.createSheet("s1").createRow(0).createCell(0).setCellValue("TokenImportCell");
            wb.write(out);
            xlsx = out.toByteArray();
        }
        HttpTestClient.Response resp = HttpTestClient.postMultipart(port(), "/ureport/import?" + credentialQuery(),
                "_excel_file", "ok.xlsx", xlsx);
        assertEquals(200, resp.status, resp.describe());
        assertTrue(resp.text().replace(" ", "").contains("\"result\":true"), resp.text());
        assertEquals(TOKEN + "|" + TENANT_ID, RecordingConfig.SEEN.get("/ureport/import"));
    }

    private static void assertNoTokenEcho(String html) throws Exception {
        assertFalse(html.contains("Tk+a"), "raw token echoed");
        assertFalse(html.contains("Tk%2Ba"), "encoded token echoed");
        assertFalse(html.contains("Tk a"), "decoded token echoed");
        assertFalse(html.contains("token="), "token parameter echoed");
    }

    private static String paramReportXml() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<ureport>\n"
                + cell("A1", 1, "'P:'+param(\"plain\")")
                + cell("A2", 2, "'T:'+param(\"tenantId\")")
                + cell("A3", 3, "'K:'+param(\"token\")")
                + "\t<row row-number=\"1\" height=\"20\"/>\n\t<row row-number=\"2\" height=\"20\"/>\n\t<row row-number=\"3\" height=\"20\"/>\n"
                + "\t<column col-number=\"1\" width=\"200\"/>\n"
                + "\t<paper type=\"A4\" orientation=\"portrait\" paging-mode=\"fitpage\"></paper>\n</ureport>";
    }

    private static String cell(String name, int row, String expr) {
        return "\t<cell expand=\"None\" name=\"" + name + "\" row=\"" + row + "\" col=\"1\">\n"
                + "\t\t<expression-value><![CDATA[" + expr + "]]></expression-value>\n"
                + "\t\t<cell-style font-size=\"10\" font-family=\"宋体\" align=\"left\" valign=\"middle\"></cell-style>\n"
                + "\t</cell>\n";
    }

}
