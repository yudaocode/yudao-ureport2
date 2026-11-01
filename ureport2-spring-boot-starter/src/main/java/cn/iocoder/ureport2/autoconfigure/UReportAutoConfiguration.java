package cn.iocoder.ureport2.autoconfigure;

import com.bstek.ureport.console.UReportServlet;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ImportResource;

import jakarta.servlet.MultipartConfigElement;

/**
 * UReport2 自动配置（boot3 线）
 *
 * 1. 导入原 ureport-console-context.xml（其中再导入 ureport-core-context.xml），保持原有 Bean 名称和 Bean 发现方式；
 * 2. 注册 {@link UReportServlet} 到 /ureport/*。
 *
 * 宿主扩展点仍为原接口：注册 ReportProvider、BuildinDatasource、Function、FontRegister 等类型的 Bean 即可被发现。
 *
 * @author 芋道源码
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(UReportServlet.class)
@ConditionalOnProperty(prefix = UReportProperties.PREFIX, name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(UReportProperties.class)
@ImportResource("classpath:ureport-console-context.xml")
public class UReportAutoConfiguration {

    public static final String SERVLET_MAPPING = UReportServlet.URL + "/*";

    static {
        UReportClasspathChecker.check(UReportAutoConfiguration.class.getClassLoader());
    }

    @Bean
    @ConditionalOnMissingBean(name = "ureportServletRegistration")
    public ServletRegistrationBean<UReportServlet> ureportServletRegistration(UReportProperties properties) {
        ServletRegistrationBean<UReportServlet> bean = new ServletRegistrationBean<>(new UReportServlet(), SERVLET_MAPPING);
        bean.setName("ureportServlet");
        // /ureport/import 使用 Servlet 原生 multipart，上限由此控制；fileSizeThreshold=0 表示超过阈值写入容器临时目录
        bean.setMultipartConfig(new MultipartConfigElement("",
                properties.getImportMaxFileSize().toBytes(), properties.getImportMaxRequestSize().toBytes(), 0));
        return bean;
    }

}
