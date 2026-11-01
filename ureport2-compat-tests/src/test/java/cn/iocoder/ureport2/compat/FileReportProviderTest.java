package cn.iocoder.ureport2.compat;

import com.bstek.ureport.exception.ReportException;
import com.bstek.ureport.provider.report.file.FileReportProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.context.support.GenericApplicationContext;

import java.io.File;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link FileReportProvider} 目录校验行为
 *
 * @author 芋道源码
 */
class FileReportProviderTest {

    private static FileReportProvider provider(String dir, boolean disabled) {
        FileReportProvider provider = new FileReportProvider();
        provider.setFileStoreDir(dir);
        provider.setDisabled(disabled);
        return provider;
    }

    @Test
    void disabledCreatesNothing(@TempDir Path tmp) {
        File dir = tmp.resolve("not-created").toFile();
        provider(dir.getAbsolutePath(), true).setApplicationContext(new GenericApplicationContext());
        provider("relative-dir-should-not-exist", true).setApplicationContext(new GenericApplicationContext());
        assertFalse(dir.exists());
        assertFalse(new File("relative-dir-should-not-exist").exists());
    }

    @Test
    void enabledRejectsRelativeAndWebInf() {
        assertThrows(ReportException.class, () -> provider("relative-dir-should-not-exist", false)
                .setApplicationContext(new GenericApplicationContext()));
        assertFalse(new File("relative-dir-should-not-exist").exists());
        assertThrows(ReportException.class, () -> provider("/WEB-INF/ureportfiles", false)
                .setApplicationContext(new GenericApplicationContext()));
        assertThrows(ReportException.class, () -> provider("", false)
                .setApplicationContext(new GenericApplicationContext()));
    }

    @Test
    void enabledAbsoluteSavesAndLoads(@TempDir Path tmp) throws Exception {
        File dir = tmp.resolve("store").toFile();
        FileReportProvider provider = provider(dir.getAbsolutePath(), false);
        provider.setApplicationContext(new GenericApplicationContext());
        assertTrue(dir.isDirectory());
        provider.saveReport("file:a.ureport.xml", "<ureport/>");
        assertEquals(1, provider.getReportFiles().size());
        assertEquals("<ureport/>", new String(org.apache.commons.io.IOUtils.toByteArray(provider.loadReport("file:a.ureport.xml")), "UTF-8"));
    }

}
