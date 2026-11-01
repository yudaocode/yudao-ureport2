package cn.iocoder.ureport2.compat;

import com.bstek.ureport.definition.datasource.BuildinDatasource;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * 宿主内置数据源：使用 Spring Boot 的内存 H2 DataSource
 *
 * @author 芋道源码
 */
@Component
public class DemoBuildinDatasource implements BuildinDatasource {

    public static final String NAME = "demo-h2";

    private final DataSource dataSource;

    public DemoBuildinDatasource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public Connection getConnection() {
        try {
            return dataSource.getConnection();
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

}
