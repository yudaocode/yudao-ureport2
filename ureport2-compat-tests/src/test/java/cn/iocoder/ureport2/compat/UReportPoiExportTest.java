package cn.iocoder.ureport2.compat;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.xmlbeans.XmlCursor;
import org.apache.xmlbeans.XmlObject;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.annotation.DirtiesContext;

import javax.xml.namespace.QName;
import java.io.ByteArrayInputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * POI 5 导出回归：芋道 JDK8 宿主使用 POI 5.x，报表库按 POI 5 编译后，
 * Excel 的背景色、边框颜色（XSSFColor）与二维码图片，Word 的表格、单元格宽度（CTTblWidth）都可以正常导出。
 *
 * 按 POI 4 编译的报表库运行在 POI 5 上时，这些导出会抛出 NoSuchMethodError
 *
 * @author 芋道源码
 */
@SpringBootTest(classes = CompatApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
// 每个测试类独立 Web 上下文：类结束后关闭，避免多个缓存上下文与 UReport 静态状态互相干扰
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class UReportPoiExportTest {

    private static final String FILE = "poi-export.ureport.xml";
    private static final String TITLE = "PoiStyledTitle";
    private static final String BODY = "PoiQrCodeLabel";
    private static final String WORD_NS = "http://schemas.openxmlformats.org/wordprocessingml/2006/main";

    private static final String BORDER = " width=\"1\" style=\"solid\" color=\"0,0,255\"/>";
    private static final String TEMPLATE = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<ureport>\n"
            // A1：背景色 + 四边框
            + "<cell expand=\"None\" name=\"A1\" row=\"1\" col=\"1\" col-span=\"2\"><simple-value><![CDATA[" + TITLE + "]]></simple-value>"
            + "<cell-style font-size=\"12\" font-family=\"宋体\" bgcolor=\"255,200,0\" align=\"center\" valign=\"middle\">"
            + "<left-border" + BORDER + "<right-border" + BORDER + "<top-border" + BORDER + "<bottom-border" + BORDER
            + "</cell-style></cell>\n"
            // A2：二维码图片，导出时按单元格的对齐方式计算图片位置
            + "<cell expand=\"None\" name=\"A2\" row=\"2\" col=\"1\">"
            + "<zxing-value source=\"text\" category=\"qrcode\" width=\"60\" height=\"60\" format=\"QR_CODE\" code-display=\"false\">"
            + "<text><![CDATA[https://www.iocoder.cn]]></text></zxing-value>"
            + "<cell-style font-size=\"10\" font-family=\"宋体\" align=\"center\" valign=\"middle\"></cell-style></cell>\n"
            + "<cell expand=\"None\" name=\"B2\" row=\"2\" col=\"2\"><simple-value><![CDATA[" + BODY + "]]></simple-value>"
            + "<cell-style font-size=\"10\" font-family=\"宋体\" align=\"center\" valign=\"middle\"></cell-style></cell>\n"
            + "<row row-number=\"1\" height=\"30\"/><row row-number=\"2\" height=\"70\"/>\n"
            + "<column col-number=\"1\" width=\"100\"/><column col-number=\"2\" width=\"120\"/>\n"
            + "<paper type=\"A4\" orientation=\"portrait\" paging-mode=\"fitpage\"></paper>\n</ureport>\n";

    @Autowired
    private Environment environment;
    @Autowired
    private MemoryReportProvider memoryReportProvider;

    @Test
    void excelExport() throws Exception {
        assertStyledExcel("/ureport/excel");
    }

    @Test
    void excelPagingExport() throws Exception {
        assertStyledExcel("/ureport/excel/paging");
    }

    @Test
    void wordExport() throws Exception {
        HttpTestClient.Response resp = export("/ureport/word");
        try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(resp.body));
             XWPFWordExtractor extractor = new XWPFWordExtractor(doc)) {
            String text = extractor.getText();
            assertTrue(text.contains(TITLE), text);
            assertTrue(text.contains(BODY), text);
            // 表格、单元格宽度：CTTblWidth.setW 在 POI 4 与 POI 5 的参数类型不同（BigInteger / Object）
            assertFalse(doc.getTables().isEmpty(), "word table missing");
            XWPFTable table = doc.getTables().get(0);
            List<BigInteger> tableWidths = getWidths(table.getCTTbl(), "./w:tblPr/w:tblW");
            assertTrue(tableWidths.stream().anyMatch(width -> width.signum() > 0), tableWidths.toString());
            List<BigInteger> cellWidths = getWidths(table.getCTTbl(), ".//w:tc/w:tcPr/w:tcW");
            assertTrue(cellWidths.stream().anyMatch(width -> width.signum() > 0), cellWidths.toString());
            // 二维码图片
            assertFalse(doc.getAllPictures().isEmpty(), "qrcode picture missing");
        }
    }

    private void assertStyledExcel(String url) throws Exception {
        HttpTestClient.Response resp = export(url);
        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(resp.body))) {
            Cell title = findCell(wb.getSheetAt(0), TITLE);
            assertNotNull(title, "title cell missing");
            assertNotNull(findCell(wb.getSheetAt(0), BODY), "body cell missing");
            // 背景色、边框颜色：new XSSFColor(Color, IndexedColorMap)
            XSSFCellStyle style = (XSSFCellStyle) title.getCellStyle();
            assertEquals(FillPatternType.SOLID_FOREGROUND, style.getFillPattern());
            assertArrayEquals(new byte[]{(byte) 255, (byte) 200, 0}, style.getFillForegroundXSSFColor().getRGB());
            assertEquals(BorderStyle.THIN, style.getBorderTop());
            assertArrayEquals(new byte[]{0, 0, (byte) 255}, style.getTopBorderXSSFColor().getRGB());
            // 二维码图片：按单元格的对齐方式（CellStyle.getAlignment / getVerticalAlignment）计算图片位置
            assertFalse(wb.getAllPictures().isEmpty(), "qrcode picture missing");
        }
    }

    private HttpTestClient.Response export(String url) throws Exception {
        memoryReportProvider.saveReport(FILE, TEMPLATE);
        HttpTestClient.Response resp = HttpTestClient.get(port(), url + "?_u=" + MemoryReportProvider.PREFIX + FILE);
        assertEquals(200, resp.status, resp.describe());
        return resp;
    }

    private static Cell findCell(Sheet sheet, String text) {
        for (Row row : sheet) {
            for (Cell cell : row) {
                if (text.equals(cell.toString())) {
                    return cell;
                }
            }
        }
        return null;
    }

    /**
     * 读取 w:w 宽度属性；不依赖精简 schema 中的具体 CT 类型，用 XPath + XmlCursor 读取
     */
    private static List<BigInteger> getWidths(XmlObject xml, String path) {
        List<BigInteger> widths = new ArrayList<>();
        for (XmlObject width : xml.selectPath("declare namespace w='" + WORD_NS + "' " + path)) {
            XmlCursor cursor = width.newCursor();
            try {
                String value = cursor.getAttributeText(new QName(WORD_NS, "w"));
                if (value != null) {
                    widths.add(new BigInteger(value));
                }
            } finally {
                cursor.dispose();
            }
        }
        return widths;
    }

    private int port() {
        return environment.getRequiredProperty("local.server.port", Integer.class);
    }

}
