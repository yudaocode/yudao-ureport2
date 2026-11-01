package cn.iocoder.ureport2.autoconfigure;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.HashMap;
import java.util.Map;

/**
 * 将 Spring Boot 风格的 {@code ureport.*} 配置（支持 kebab-case 等宽松绑定）
 * 转换为原 ureport-core-context.xml / ureport-console-context.xml 使用的驼峰占位符。
 *
 * 该 PropertySource 放在最后，宿主直接配置的驼峰键仍然优先；
 * 它又高于 classpath:ureport.properties（XML 中的本地默认值），因此默认值由 {@link UReportProperties} 决定。
 *
 * @author 芋道源码
 */
public class UReportEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    public static final String PROPERTY_SOURCE_NAME = "yudaoUReportProperties";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        UReportProperties properties = Binder.get(environment)
                .bind(UReportProperties.PREFIX, UReportProperties.class)
                .orElseGet(UReportProperties::new);
        Map<String, Object> map = new HashMap<>();
        map.put("ureport.debug", String.valueOf(properties.isDebug()));
        map.put("ureport.disableFileProvider", String.valueOf(properties.isDisableFileProvider()));
        map.put("ureport.disableHttpSessionReportCache", String.valueOf(properties.isDisableHttpSessionReportCache()));
        // 未配置时给空串，由 FileReportProvider 在启用时报错，而不是回落到 /WEB-INF/ureportfiles
        map.put("ureport.fileStoreDir", properties.getFileStoreDir() == null ? "" : properties.getFileStoreDir());
        environment.getPropertySources().remove(PROPERTY_SOURCE_NAME);
        environment.getPropertySources().addLast(new MapPropertySource(PROPERTY_SOURCE_NAME, map));
    }

    @Override
    public int getOrder() {
        // 在 Boot 加载 application.yaml 之后执行
        return Ordered.LOWEST_PRECEDENCE;
    }

}
