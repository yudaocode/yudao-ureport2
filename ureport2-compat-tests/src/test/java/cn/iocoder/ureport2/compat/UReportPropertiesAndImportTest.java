package cn.iocoder.ureport2.compat;

import com.bstek.ureport.Utils;
import com.bstek.ureport.console.cache.HttpSessionReportCache;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 非默认 ureport.* 配置映射（验证 EnvironmentPostProcessor 生效）与 /ureport/import multipart 行为
 *
 * @author 芋道源码
 */
@SpringBootTest(classes = CompatApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "ureport.debug=true",
                "ureport.disable-http-session-report-cache=true",
                "ureport.import-max-file-size=20KB",
                "ureport.import-max-request-size=30KB"
        })
class UReportPropertiesAndImportTest {

    @Autowired
    private ApplicationContext applicationContext;
    @Autowired
    private Environment environment;

    private int port() {
        return environment.getRequiredProperty("local.server.port", Integer.class);
    }

    @Test
    void kebabCasePropertiesReachOriginalXmlPlaceholders() {
        assertTrue(Utils.isDebug(), "ureport.debug=true should reach ${ureport.debug}");
        HttpSessionReportCache cache = applicationContext.getBean("ureport.httpSessionReportCache", HttpSessionReportCache.class);
        assertTrue(cache.disabled(), "ureport.disable-http-session-report-cache should reach ${ureport.disableHttpSessionReportCache}");
    }

    @Test
    void importValidXlsx() throws Exception {
        byte[] xlsx;
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            XSSFSheet sheet = wb.createSheet("s1");
            sheet.createRow(0).createCell(0).setCellValue("ImportedCellA1");
            sheet.createRow(1).createCell(1).setCellValue("ImportedCellB2");
            wb.write(out);
            xlsx = out.toByteArray();
        }
        HttpTestClient.Response resp = HttpTestClient.postMultipart(port(), "/ureport/import", "_excel_file", "ok.xlsx", xlsx);
        assertEquals(200, resp.status);
        assertTrue(resp.text().replace(" ", "").contains("\"result\":true"), resp.text());
    }

    @Test
    void importRejectsNonExcelFile() throws Exception {
        HttpTestClient.Response resp = HttpTestClient.postMultipart(port(), "/ureport/import", "_excel_file", "evil.txt",
                "not excel".getBytes(StandardCharsets.UTF_8));
        assertEquals(200, resp.status);
        String text = resp.text().replace(" ", "");
        assertTrue(text.contains("\"result\":false"), resp.text());
        assertTrue(text.contains("errorInfo"), resp.text());
    }

    @Test
    void importRejectsCorruptXlsx() throws Exception {
        HttpTestClient.Response resp = HttpTestClient.postMultipart(port(), "/ureport/import", "_excel_file", "broken.xlsx",
                "PK-not-a-real-zip".getBytes(StandardCharsets.UTF_8));
        assertTrue(resp.text().replace(" ", "").contains("\"result\":false"), resp.text());
    }

    @Test
    void importRejectsOversizedFile() throws Exception {
        byte[] big = new byte[100 * 1024];
        HttpTestClient.Response resp = HttpTestClient.postMultipart(port(), "/ureport/import", "_excel_file", "big.xlsx", big);
        String text = resp.text().replace(" ", "");
        assertTrue(text.contains("\"result\":false"), "status=" + resp.status + " body=" + resp.text());
    }

}
