package cn.iocoder.ureport2.compat;

import cn.iocoder.ureport2.autoconfigure.UReportClasspathChecker;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 混合线 / 双引擎检测
 *
 * @author 芋道源码
 */
class UReportClasspathCheckerTest {

    @Test
    void currentClasspathPasses() {
        assertDoesNotThrow(() -> UReportClasspathChecker.check(getClass().getClassLoader()));
    }

    @Test
    void otherLineMarkerFails() throws Exception {
        Path dir = Files.createTempDirectory("yudao-ureport2-checker");
        Path marker = dir.resolve("META-INF/yudao-ureport2/core.properties");
        Files.createDirectories(marker.getParent());
        Files.write(marker, "line=boot2\n".getBytes(StandardCharsets.UTF_8));
        try (URLClassLoader loader = new URLClassLoader(new URL[]{dir.toUri().toURL()}, getClass().getClassLoader())) {
            IllegalStateException e = assertThrows(IllegalStateException.class, () -> UReportClasspathChecker.check(loader));
            assertTrue(e.getMessage().contains("boot2"), e.getMessage());
        } finally {
            Files.delete(marker);
        }
    }

    @Test
    void duplicateEngineClassFails() throws Exception {
        Path dir = Files.createTempDirectory("yudao-ureport2-checker");
        Path clazz = dir.resolve("com/bstek/ureport/Utils.class");
        Files.createDirectories(clazz.getParent());
        Files.write(clazz, new byte[]{0});
        try (URLClassLoader loader = new URLClassLoader(new URL[]{dir.toUri().toURL()}, getClass().getClassLoader())) {
            IllegalStateException e = assertThrows(IllegalStateException.class, () -> UReportClasspathChecker.check(loader));
            assertTrue(e.getMessage().contains("com/bstek/ureport/Utils.class"), e.getMessage());
        } finally {
            new File(clazz.toUri()).delete();
        }
    }

}
