package cn.iocoder.ureport2.compat;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 最小宿主应用：仅引入 starter，验证自动配置、/ureport/* 与原模板渲染
 *
 * @author 芋道源码
 */
@SpringBootApplication
public class CompatApplication {

    public static void main(String[] args) {
        SpringApplication.run(CompatApplication.class, args);
    }

}
