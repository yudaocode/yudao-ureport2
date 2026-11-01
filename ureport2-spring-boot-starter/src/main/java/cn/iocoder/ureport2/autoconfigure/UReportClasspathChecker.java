package cn.iocoder.ureport2.autoconfigure;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

/**
 * 启动时检查 classpath，阻止以下情况：
 * 1. 同时存在官方 com.bstek.ureport 的 jar（同包名双引擎）；
 * 2. 混入其他 Boot 线的 cn.iocoder.ureport2 core / console（artifactId 相同，版本主号不同）。
 *
 * @author 芋道源码
 */
public final class UReportClasspathChecker {

    public static final String EXPECTED_LINE = "boot4";

    private static final String[] CLASS_MARKERS = {
            "com/bstek/ureport/Utils.class",
            "com/bstek/ureport/console/UReportServlet.class"
    };
    private static final String[] LINE_MARKERS = {
            "META-INF/yudao-ureport2/core.properties",
            "META-INF/yudao-ureport2/console.properties"
    };

    private UReportClasspathChecker() {
    }

    public static void check(ClassLoader classLoader) {
        List<String> problems = new ArrayList<>();
        for (String marker : CLASS_MARKERS) {
            List<URL> urls = list(classLoader, marker);
            if (urls.size() > 1) {
                problems.add("Found " + urls.size() + " copies of " + marker + ": " + urls);
            }
        }
        for (String marker : LINE_MARKERS) {
            List<URL> urls = list(classLoader, marker);
            if (urls.isEmpty()) {
                problems.add("Missing " + marker + ", the official com.bstek.ureport jar may be used instead of the "
                        + "cn.iocoder.ureport2 " + EXPECTED_LINE + " line");
            }
            for (URL url : urls) {
                String line = readLine(url);
                if (!EXPECTED_LINE.equals(line)) {
                    problems.add(marker + " at " + url + " belongs to line [" + line + "], expected [" + EXPECTED_LINE + "]");
                }
            }
        }
        if (!problems.isEmpty()) {
            throw new IllegalStateException("yudao-ureport2 classpath conflict. Remove com.bstek.ureport:* and make sure "
                    + "all cn.iocoder.ureport2:ureport2-* artifacts use the same version of the " + EXPECTED_LINE
                    + " line (other Spring Boot lines share the same artifactIds with a different major version)."
                    + " Details: " + problems);
        }
    }

    private static List<URL> list(ClassLoader classLoader, String name) {
        try {
            return Collections.list(classLoader.getResources(name));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to scan classpath for " + name, e);
        }
    }

    private static String readLine(URL url) {
        Properties properties = new Properties();
        try (InputStream in = url.openStream()) {
            properties.load(in);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read " + url, e);
        }
        return properties.getProperty("line");
    }

}
