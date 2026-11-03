package cn.iocoder.ureport2.compat;

import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.parser.PdfTextExtractor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.annotation.DirtiesContext;

import java.io.ByteArrayInputStream;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 预览页「预览」「分页预览」切换链接：按真实请求链路，从预览页 HTML 中解析下一跳地址并请求，
 * 校验切换后，以及切换后的打印、刷新、导出、PDF 在线预览打印调整纸张，都仍加载入口的模板
 *
 * 入口与宿主前端一致：存储前缀不编码，模板名称 encodeURIComponent 三次（Servlet 解码一次，BaseServletAction#decode 再解码两次）。
 * 同时保存 +、空格、字面 %20 三个容易互相混淆的模板，多解码或少解码一次，都会读到其它模板
 *
 * @author 芋道源码
 */
@SpringBootTest(classes = CompatApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
// 每个测试类独立 Web 上下文：类结束后关闭，避免多个缓存上下文与 UReport 静态状态互相干扰
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class UReportPreviewNavigationTest {

    /**
     * 模板名称（不包含存储前缀）与模板内容中的唯一标记
     */
    private static final Map<String, String> REPORTS = new LinkedHashMap<>();

    static {
        REPORTS.put("Nav+Case.ureport.xml", "NavMarkerLiteralPlus");
        REPORTS.put("Nav Case.ureport.xml", "NavMarkerActualSpace");
        REPORTS.put("Nav%20Case.ureport.xml", "NavMarkerLiteralPercent20");
        REPORTS.put("导航报表.ureport.xml", "NavMarkerChinese");
        REPORTS.put("nav-plain.ureport.xml", "NavMarkerPlain");
    }

    /**
     * 预览页工具栏中的切换链接
     */
    private static final Pattern LINK = Pattern.compile("<a href=\"([^\"]*)\"[^>]*>(预览|分页预览)</a>");
    /**
     * 含 +、空格、中文等需编码字符的 token，宿主前端透传
     */
    private static final String TOKEN = "Tk+nav/1 中";

    @Autowired
    private Environment environment;
    @Autowired
    private MemoryReportProvider memoryReportProvider;

    private int port() {
        return environment.getRequiredProperty("local.server.port", Integer.class);
    }

    @BeforeEach
    void saveReports() {
        REPORTS.forEach((name, marker) -> memoryReportProvider.saveReport(name, markerReportXml(marker)));
    }

    @Test
    void switchLinksKeepTemplate() throws Exception {
        for (Map.Entry<String, String> report : REPORTS.entrySet()) {
            // 默认工具栏、通过 _t 指定的工具栏（9 为分页），分别对应模板中的两组链接
            for (String tools : new String[]{"", "&_t=9"}) {
                String name = report.getKey();
                String marker = report.getValue();
                String entry = "/ureport/preview?_u=" + entryFile(name) + tools + "&plain=PlainValue42";
                String html = assertLoads(entry, marker, name);

                // 预览 -> 预览、分页预览
                Map<String, String> links = parseLinks(html, name);
                assertLoads(links.get("预览"), marker, name);
                String pagingHtml = assertLoads(links.get("分页预览"), marker, name);
                // 分页预览 -> 预览、分页预览：多次切换，仍是同一个模板
                Map<String, String> pagingLinks = parseLinks(pagingHtml, name);
                assertLoads(pagingLinks.get("预览"), marker, name);
                assertLoads(pagingLinks.get("分页预览"), marker, name);

                // 链接保留工具栏与普通参数
                for (String link : links.values()) {
                    assertEquals(!tools.isEmpty(), link.contains("&_t=9&"), link);
                    assertTrue(link.contains("plain=PlainValue42"), link);
                }
            }
        }
    }

    @Test
    void printAndExportAfterSwitchKeepTemplate() throws Exception {
        for (Map.Entry<String, String> report : REPORTS.entrySet()) {
            String name = report.getKey();
            String marker = report.getValue();
            String html = assertLoads("/ureport/preview?_u=" + entryFile(name), marker, name);
            // 切换到分页预览后，preview.bundle.js 的打印、刷新、导出，都把地址栏的 query 原样拼到对应地址后面
            String link = parseLinks(html, name).get("分页预览");
            assertLoads(link, marker, name);
            String query = link.substring(link.indexOf('?'));

            assertResponseContains("/ureport/preview/loadData" + query, marker, name);
            assertResponseContains("/ureport/preview/loadPrintPages" + query, marker, name);
            HttpTestClient.Response excel = HttpTestClient.get(port(), "/ureport/excel" + query);
            assertEquals(200, excel.status, excel.describe());
            String cells = excelText(excel.body);
            assertTrue(cells.contains(marker), name + " -> " + cells);
            assertOnlyMarker(cells, marker, name);
        }
    }

    @Test
    void pdfNewPagingKeepTemplate() throws Exception {
        for (Map.Entry<String, String> report : REPORTS.entrySet()) {
            String name = report.getKey();
            String marker = report.getValue();
            String entry = "/ureport/preview?_u=" + entryFile(name);
            String html = assertLoads(entry, marker, name);
            String link = parseLinks(html, name).get("分页预览");
            assertLoads(link, marker, name);
            assertFalse(link.contains("token="), "token 不应回显到切换链接：" + link);
            // 入口、切换到分页预览后的地址栏 query；ureport-token.js 会为后续请求附加 token、tenantId
            for (String query : new String[]{entry.substring(entry.indexOf('?')), link.substring(link.indexOf('?'))}) {
                query += "&token=" + URLEncoder.encode(TOKEN, "UTF-8") + "&tenantId=1";
                assertPdfNewPaging(query, marker, name);
            }
        }
    }

    /**
     * 按「PDF 在线预览打印」的请求顺序：读取纸张设置，调整后提交 /pdf/newPaging，再通过 /pdf/show 预览 PDF
     */
    private void assertPdfNewPaging(String query, String marker, String name) throws Exception {
        HttpTestClient.Response paper = HttpTestClient.get(port(), "/ureport/preview/loadPagePaper" + query);
        assertEquals(200, paper.status, name + " -> " + paper.describe());
        String paperJson = paper.text();
        assertTrue(paperJson.trim().startsWith("{") && paperJson.contains("paperType"), name + " -> " + paperJson);

        // 与 jQuery 提交表单一致，_paper 作为表单参数；_u 等仍在 query 中
        HttpTestClient.Response newPaging = HttpTestClient.postForm(port(), "/ureport/pdf/newPaging" + query,
                "_paper=" + URLEncoder.encode(paperJson, "UTF-8"));
        assertEquals(200, newPaging.status, name + " -> " + newPaging.describe());

        HttpTestClient.Response pdf = HttpTestClient.get(port(), "/ureport/pdf/show" + query + "&_r=0");
        assertEquals(200, pdf.status, name + " -> " + pdf.describe());
        assertTrue(pdf.contentType.startsWith("application/pdf"), name + " -> " + pdf.contentType);
        assertEquals("%PDF", new String(pdf.body, 0, 4, "ISO-8859-1"), name);
        PdfReader reader = new PdfReader(pdf.body);
        try {
            String text = PdfTextExtractor.getTextFromPage(reader, 1);
            assertTrue(text.contains(marker), name + " -> " + text);
            assertOnlyMarker(text, marker, name);
        } finally {
            reader.close();
        }
    }

    /**
     * 请求预览页，并断言加载的是指定模板
     */
    private String assertLoads(String path, String marker, String name) throws Exception {
        assertNotNull(path, name + "：没有找到切换链接");
        HttpTestClient.Response resp = HttpTestClient.get(port(), path);
        assertEquals(200, resp.status, name + " -> " + resp.describe());
        String html = resp.text();
        assertTrue(html.contains(marker), name + " -> " + path + " 没有加载入口模板：" + html);
        assertOnlyMarker(html, marker, name);
        return html;
    }

    private void assertResponseContains(String path, String marker, String name) throws Exception {
        HttpTestClient.Response resp = HttpTestClient.get(port(), path);
        assertEquals(200, resp.status, name + " -> " + resp.describe());
        assertTrue(resp.text().contains(marker), name + " -> " + resp.describe());
        assertOnlyMarker(resp.text(), marker, name);
    }

    private static void assertOnlyMarker(String text, String marker, String name) {
        for (String other : REPORTS.values()) {
            if (!other.equals(marker)) {
                assertFalse(text.contains(other), name + " 读到了其它模板：" + other);
            }
        }
    }

    /**
     * 从预览页 HTML 中解析「预览」「分页预览」链接；href 由服务端输出，按浏览器点击时的地址原样使用
     */
    private static Map<String, String> parseLinks(String html, String name) {
        Map<String, String> links = new LinkedHashMap<>();
        Matcher matcher = LINK.matcher(html);
        while (matcher.find()) {
            assertNull(links.put(matcher.group(2), matcher.group(1)), name + "：切换链接重复");
        }
        assertEquals(2, links.size(), name + "：切换链接缺失 " + html);
        for (String link : links.values()) {
            assertTrue(link.startsWith("/ureport/preview?_u="), link);
        }
        return links;
    }

    /**
     * 宿主前端打开预览页时的 _u：存储前缀不编码，模板名称 encodeURIComponent 三次
     */
    private static String entryFile(String name) throws Exception {
        String value = name;
        for (int i = 0; i < 3; i++) {
            value = encodeURIComponent(value);
        }
        return MemoryReportProvider.PREFIX + value;
    }

    /**
     * 与浏览器 encodeURIComponent 一致：空格编码为 %20；本测试的模板名称不包含两者有差异的 !'()~ 字符
     */
    private static String encodeURIComponent(String value) throws Exception {
        return URLEncoder.encode(value, "UTF-8").replace("+", "%20");
    }

    private static String excelText(byte[] body) throws Exception {
        List<String> values = new ArrayList<>();
        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(body))) {
            for (Row row : wb.getSheetAt(0)) {
                for (Cell cell : row) {
                    values.add(cell.toString());
                }
            }
        }
        return String.join("|", values);
    }

    private static String markerReportXml(String marker) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<ureport>\n"
                + "\t<cell expand=\"None\" name=\"A1\" row=\"1\" col=\"1\">\n"
                + "\t\t<simple-value><![CDATA[" + marker + "]]></simple-value>\n"
                + "\t\t<cell-style font-size=\"10\" font-family=\"宋体\" align=\"left\" valign=\"middle\"></cell-style>\n"
                + "\t</cell>\n"
                + "\t<row row-number=\"1\" height=\"20\"/>\n"
                + "\t<column col-number=\"1\" width=\"200\"/>\n"
                + "\t<paper type=\"A4\" orientation=\"portrait\" paging-mode=\"fitpage\"></paper>\n</ureport>";
    }

}
