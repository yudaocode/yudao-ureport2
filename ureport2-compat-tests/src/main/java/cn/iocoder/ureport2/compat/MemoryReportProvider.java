package cn.iocoder.ureport2.compat;

import com.bstek.ureport.exception.ReportException;
import com.bstek.ureport.provider.report.ReportFile;
import com.bstek.ureport.provider.report.ReportProvider;
import org.apache.commons.io.IOUtils;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 宿主自定义存储 Provider（内存），启动时从 classpath:reports/ 预置原 XML 模板
 *
 * @author 芋道源码
 */
@Component
public class MemoryReportProvider implements ReportProvider {

    public static final String PREFIX = "mem:";

    private final Map<String, String> reports = new ConcurrentHashMap<>();

    public MemoryReportProvider() {
        try (InputStream in = MemoryReportProvider.class.getResourceAsStream("/reports/demo.ureport.xml")) {
            reports.put("demo.ureport.xml", IOUtils.toString(in, StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String strip(String file) {
        return file.startsWith(PREFIX) ? file.substring(PREFIX.length()) : file;
    }

    @Override
    public InputStream loadReport(String file) {
        String content = reports.get(strip(file));
        if (content == null) {
            throw new ReportException("Report not found: " + file);
        }
        return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public void deleteReport(String file) {
        reports.remove(strip(file));
    }

    @Override
    public List<ReportFile> getReportFiles() {
        List<ReportFile> list = new ArrayList<>();
        for (String name : reports.keySet()) {
            list.add(new ReportFile(name, new Date()));
        }
        return list;
    }

    @Override
    public void saveReport(String file, String content) {
        reports.put(strip(file), content);
    }

    @Override
    public String getName() {
        return "宿主内存存储";
    }

    @Override
    public boolean disabled() {
        return false;
    }

    @Override
    public String getPrefix() {
        return PREFIX;
    }

}
