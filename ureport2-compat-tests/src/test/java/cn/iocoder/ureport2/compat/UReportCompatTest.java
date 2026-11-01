package cn.iocoder.ureport2.compat;

import com.bstek.ureport.Utils;
import com.bstek.ureport.provider.report.ReportProvider;
import com.bstek.ureport.provider.report.file.FileReportProvider;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.parser.PdfTextExtractor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.xmlbeans.XmlCursor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 兼容性验收：自动配置、/ureport/* 路由、原 XML 模板预览与 Excel/PDF/Word 导出、
 * 内置数据源（JDBC）/ Spring Bean 数据集 / 自定义函数 / 宿主 Provider 扩展，以及默认配置。
 *
 * @author 芋道源码
 */
@SpringBootTest(classes = CompatApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UReportCompatTest {

    private static final String REPORT = "mem:demo.ureport.xml";
    private static final String WORD_NS = "http://schemas.openxmlformats.org/wordprocessingml/2006/main";

    @Autowired
    private ApplicationContext applicationContext;
    @Autowired
    private Environment environment;

    private int port() {
        return environment.getRequiredProperty("local.server.port", Integer.class);
    }

    @Test
    void defaultsAreSafe() {
        assertFalse(Utils.isDebug(), "ureport.debug should default to false");
        FileReportProvider fileProvider = applicationContext.getBean("ureport.fileReportProvider", FileReportProvider.class);
        assertTrue(fileProvider.disabled(), "file provider should be disabled by default");
    }

    @Test
    void hostProviderIsDiscovered() {
        Map<String, ReportProvider> providers = applicationContext.getBeansOfType(ReportProvider.class);
        assertTrue(providers.values().stream().anyMatch(p -> p instanceof MemoryReportProvider));
    }

    @Test
    void designerPage() throws Exception {
        HttpTestClient.Response resp = HttpTestClient.get(port(), "/ureport/designer");
        assertEquals(200, resp.status);
        assertTrue(resp.text().contains("ureport"), resp.text());
        HttpTestClient.Response js = HttpTestClient.get(port(), "/ureport/res/ureport-asserts/js/designer.bundle.js");
        assertEquals(200, js.status);
        assertTrue(js.body.length > 10000);
    }

    @Test
    void htmlPreview() throws Exception {
        HttpTestClient.Response resp = HttpTestClient.get(port(), "/ureport/preview?_u=" + REPORT);
        assertEquals(200, resp.status);
        String html = resp.text();
        assertTrue(html.contains("CompatReportTitle"), html);
        assertTrue(html.contains("JdbcEmpAlice") && html.contains("JdbcEmpCarol"), "JDBC buildin datasource rows");
        assertTrue(html.contains("BeanDeptAlpha") && html.contains("BeanDeptBeta"), "Spring bean dataset rows");
        assertTrue(html.contains(DemoFunction.RESULT), "custom function");
        assertTrue(html.contains("6500.5"), "sum(C2) total");
    }

    @Test
    void excelExport() throws Exception {
        HttpTestClient.Response resp = HttpTestClient.get(port(), "/ureport/excel?_u=" + REPORT);
        assertEquals(200, resp.status);
        List<String> values = new ArrayList<>();
        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(resp.body))) {
            Sheet sheet = wb.getSheetAt(0);
            for (Row row : sheet) {
                for (Cell cell : row) {
                    values.add(cell.toString());
                }
            }
        }
        String all = String.join("|", values);
        assertTrue(all.contains("CompatReportTitle"), all);
        assertTrue(all.contains("JdbcEmpBob"), all);
        assertTrue(all.contains("BeanDeptBeta"), all);
        assertTrue(all.contains("6500.5"), all);
    }

    @Test
    void pdfExport() throws Exception {
        HttpTestClient.Response resp = HttpTestClient.get(port(), "/ureport/pdf?_u=" + REPORT);
        assertEquals(200, resp.status);
        assertEquals("%PDF", new String(resp.body, 0, 4, "ISO-8859-1"));
        PdfReader reader = new PdfReader(resp.body);
        try {
            assertTrue(reader.getNumberOfPages() >= 1);
            String text = PdfTextExtractor.getTextFromPage(reader, 1);
            assertTrue(text.contains("CompatReportTitle"), text);
            assertTrue(text.contains("JdbcEmpCarol"), text);
            assertTrue(text.contains(DemoFunction.RESULT), text);
        } finally {
            reader.close();
        }
    }

    @Test
    void wordExport() throws Exception {
        HttpTestClient.Response resp = HttpTestClient.get(port(), "/ureport/word?_u=" + REPORT);
        assertEquals(200, resp.status);
        try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(resp.body));
             XWPFWordExtractor extractor = new XWPFWordExtractor(doc)) {
            String text = extractor.getText();
            assertTrue(text.contains("CompatReportTitle"), text);
            assertTrue(text.contains("JdbcEmpAlice"), text);
            // WordProducer 用 XmlCursor 写入的页面设置（POI 4/5 精简 schema 无 CTPageSz）
            String sectPr = doc.getDocument().getBody().getSectPr().xmlText();
            assertTrue(sectPr.contains("pgSz") && sectPr.contains("pgMar"), sectPr);
            // 直接子元素检查：pgSz、pgMar 必须是 sectPr 的子元素，而不只是出现在 XML 文本中。
            // boot2 的 poi-ooxml-schemas 4.1.2 精简包没有 CTPageSz / CTPageMar 类，不能调用 getPgSz()，改用 XmlCursor 遍历子元素
            List<String> children = new ArrayList<>();
            XmlCursor cursor = doc.getDocument().getBody().getSectPr().newCursor();
            try {
                for (boolean has = cursor.toFirstChild(); has; has = cursor.toNextSibling()) {
                    if (WORD_NS.equals(cursor.getName().getNamespaceURI())) {
                        children.add(cursor.getName().getLocalPart());
                    }
                }
            } finally {
                cursor.dispose();
            }
            assertTrue(children.contains("pgSz"), children.toString());
            assertTrue(children.contains("pgMar"), children.toString());
        }
    }

}
