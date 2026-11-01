package cn.iocoder.ureport2.compat;

import com.bstek.ureport.export.pdf.font.FontRegister;
import com.itextpdf.text.pdf.PdfDictionary;
import com.itextpdf.text.pdf.PdfName;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.parser.PdfTextExtractor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.test.annotation.DirtiesContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 字体模块回归：原 ureport-font-context.xml 注册的字体可被发现，模板中的中文用“宋体”导出 PDF 后，
 * 文本可以提取，且 PDF 内嵌了该字体的子集。
 *
 * @author 芋道源码
 */
@SpringBootTest(classes = CompatApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
// 每个测试类独立 Web 上下文：类结束后关闭，避免多个缓存上下文与 UReport 静态状态互相干扰
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class UReportFontRestorationTest {

    private static final String FILE = "zh-font.ureport.xml";
    private static final String TITLE = "中文报表标题";
    private static final String BODY = "宋体字形测试";

    private static final String TEMPLATE = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<ureport>\n"
            + "<cell expand=\"None\" name=\"A1\" row=\"1\" col=\"1\"><simple-value><![CDATA[" + TITLE + "]]></simple-value>"
            + "<cell-style font-size=\"14\" font-family=\"宋体\" align=\"center\" valign=\"middle\"></cell-style></cell>\n"
            + "<cell expand=\"None\" name=\"A2\" row=\"2\" col=\"1\"><simple-value><![CDATA[" + BODY + "]]></simple-value>"
            + "<cell-style font-size=\"10\" font-family=\"宋体\" align=\"center\" valign=\"middle\"></cell-style></cell>\n"
            + "<row row-number=\"1\" height=\"30\"/><row row-number=\"2\" height=\"20\"/>\n"
            + "<column col-number=\"1\" width=\"200\"/>\n"
            + "<paper type=\"A4\" orientation=\"portrait\" paging-mode=\"fitpage\"></paper>\n</ureport>\n";

    @Autowired
    private ApplicationContext applicationContext;
    @Autowired
    private Environment environment;
    @Autowired
    private MemoryReportProvider memoryReportProvider;

    @Test
    void originalFontRegistersAreLoaded() throws Exception {
        // 原 ureport-font-context.xml 中声明的 Bean 名称
        String[] beanNames = {"ureport.arialFontRegister", "ureport.comicSansMSFontRegister", "ureport.courierNewFontRegister",
                "ureport.fangSongFontRegister", "ureport.heiTiFontRegister", "ureport.kaiTiFontRegister",
                "ureport.songTiFontRegister", "ureport.timesNewRomanFontRegister", "ureport.yaheiFontRegister"};
        for (String beanName : beanNames) {
            FontRegister register = applicationContext.getBean(beanName, FontRegister.class);
            Resource resource = applicationContext.getResource("classpath:" + register.getFontPath());
            assertTrue(resource.exists(), beanName + " font file missing: " + register.getFontPath());
            assertTrue(resource.contentLength() > 10000, beanName + " font file too small");
        }
        Map<String, FontRegister> all = applicationContext.getBeansOfType(FontRegister.class);
        assertTrue(all.values().stream().anyMatch(r -> "宋体".equals(r.getFontName())));
    }

    @Test
    void chinesePdfUsesEmbeddedSongTi() throws Exception {
        memoryReportProvider.saveReport(FILE, TEMPLATE);
        int port = environment.getRequiredProperty("local.server.port", Integer.class);
        HttpTestClient.Response resp = HttpTestClient.get(port, "/ureport/pdf?_u=" + MemoryReportProvider.PREFIX + FILE);
        assertEquals(200, resp.status, resp.describe());
        assertEquals("%PDF", new String(resp.body, 0, 4, "ISO-8859-1"));
        PdfReader reader = new PdfReader(resp.body);
        try {
            String text = PdfTextExtractor.getTextFromPage(reader, 1);
            assertTrue(text.contains(TITLE), text);
            assertTrue(text.contains(BODY), text);
            List<String> embedded = new ArrayList<>();
            PdfDictionary fonts = reader.getPageN(1).getAsDict(PdfName.RESOURCES).getAsDict(PdfName.FONT);
            assertNotNull(fonts, "page has no font resources");
            for (PdfName key : fonts.getKeys()) {
                PdfDictionary font = fonts.getAsDict(key);
                PdfName baseFont = font.getAsName(PdfName.BASEFONT);
                if (font.getAsArray(PdfName.DESCENDANTFONTS) == null) {
                    continue;
                }
                PdfDictionary descendant = font.getAsArray(PdfName.DESCENDANTFONTS).getAsDict(0);
                PdfDictionary descriptor = descendant.getAsDict(PdfName.FONTDESCRIPTOR);
                if (descriptor != null && (descriptor.get(PdfName.FONTFILE2) != null || descriptor.get(PdfName.FONTFILE3) != null)) {
                    embedded.add(String.valueOf(baseFont));
                }
            }
            // 子集字体名形如 /ABCDEF+SimSun
            assertTrue(embedded.stream().anyMatch(n -> n.matches("/[A-Z]{6}\\+.*")), "no embedded subset font: " + embedded);
        } finally {
            reader.close();
        }
    }

}
