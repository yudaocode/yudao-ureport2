package cn.iocoder.ureport2.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

/**
 * UReport2 配置项，前缀 {@code ureport}
 *
 * 这些值会被 {@link UReportEnvironmentPostProcessor} 映射为原 XML 中使用的占位符，
 * 例如 {@code ureport.disable-file-provider} 映射为 {@code ${ureport.disableFileProvider}}。
 *
 * @author 芋道源码
 */
@ConfigurationProperties(prefix = UReportProperties.PREFIX)
public class UReportProperties {

    public static final String PREFIX = "ureport";

    /**
     * 是否启用 UReport2 自动配置
     */
    private boolean enabled = true;
    /**
     * 是否输出调试信息（原 ureport.debug），默认关闭
     */
    private boolean debug = false;
    /**
     * 是否禁用文件存储 Provider（原 ureport.disableFileProvider），默认禁用，避免向 /WEB-INF 写文件
     */
    private boolean disableFileProvider = true;
    /**
     * 文件存储目录（原 ureport.fileStoreDir），启用文件存储时必须配置为绝对路径
     */
    private String fileStoreDir;
    /**
     * 是否禁用 HttpSession 报表缓存（原 ureport.disableHttpSessionReportCache）
     */
    private boolean disableHttpSessionReportCache = false;
    /**
     * /ureport/import 单个上传文件大小上限，默认 10MB
     */
    private DataSize importMaxFileSize = DataSize.ofMegabytes(10);
    /**
     * /ureport/import 整个 multipart 请求大小上限，默认 11MB
     */
    private DataSize importMaxRequestSize = DataSize.ofMegabytes(11);

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isDebug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public boolean isDisableFileProvider() {
        return disableFileProvider;
    }

    public void setDisableFileProvider(boolean disableFileProvider) {
        this.disableFileProvider = disableFileProvider;
    }

    public String getFileStoreDir() {
        return fileStoreDir;
    }

    public void setFileStoreDir(String fileStoreDir) {
        this.fileStoreDir = fileStoreDir;
    }

    public boolean isDisableHttpSessionReportCache() {
        return disableHttpSessionReportCache;
    }

    public void setDisableHttpSessionReportCache(boolean disableHttpSessionReportCache) {
        this.disableHttpSessionReportCache = disableHttpSessionReportCache;
    }

    public DataSize getImportMaxFileSize() {
        return importMaxFileSize;
    }

    public void setImportMaxFileSize(DataSize importMaxFileSize) {
        this.importMaxFileSize = importMaxFileSize;
    }

    public DataSize getImportMaxRequestSize() {
        return importMaxRequestSize;
    }

    public void setImportMaxRequestSize(DataSize importMaxRequestSize) {
        this.importMaxRequestSize = importMaxRequestSize;
    }

}
