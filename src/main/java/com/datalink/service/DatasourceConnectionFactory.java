package com.datalink.service;

import com.datalink.model.DlDatasource;
import com.datalink.util.CryptoUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * 数据源连接工厂 — 根据数据源类型生成对应 JDBC 连接
 * 支持：MySQL、PostgreSQL、ClickHouse、Oracle、Hive
 */
@Slf4j
@Service
public class DatasourceConnectionFactory {

    @Value("${datalink.crypto.key}")
    private String cryptoKey;

    /**
     * 根据数据源配置创建 JDBC 连接
     */
    public Connection getConnection(DlDatasource ds) throws SQLException {
        String password = CryptoUtil.decryptSafe(ds.getPassword(), cryptoKey);
        String url = buildJdbcUrl(ds);
        log.debug("建立数据源连接: type={}, host={}:{}", ds.getType(), ds.getHost(), ds.getPort());
        return DriverManager.getConnection(url, ds.getUsername(), password);
    }

    /**
     * 根据数据源类型构建 JDBC URL
     */
    public String buildJdbcUrl(DlDatasource ds) {
        String type = ds.getType() != null ? ds.getType().toLowerCase() : "mysql";
        String host = ds.getHost();
        int port = ds.getPort() != null ? ds.getPort() : defaultPort(type);
        String db = ds.getDatabaseName() != null ? ds.getDatabaseName() : "";
        String extra = ds.getExtraParams() != null ? ds.getExtraParams() : "";

        switch (type) {
            case "mysql":
                return "jdbc:mysql://" + host + ":" + port + "/" + db
                        + "?useUnicode=true&characterEncoding=UTF-8&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai"
                        + (extra.isEmpty() ? "" : "&" + extra);
            case "postgresql":
            case "postgres":
                return "jdbc:postgresql://" + host + ":" + port + "/" + db
                        + "?useSSL=false"
                        + (extra.isEmpty() ? "" : "&" + extra);
            case "clickhouse":
                return "jdbc:clickhouse://" + host + ":" + port + "/" + db
                        + (extra.isEmpty() ? "" : "?" + extra);
            case "oracle":
                return "jdbc:oracle:thin:@" + host + ":" + port + ":" + db;
            case "hive":
                return "jdbc:hive2://" + host + ":" + port + "/" + db
                        + (extra.isEmpty() ? "" : ";" + extra);
            default:
                throw new IllegalArgumentException("不支持的数据源类型: " + ds.getType());
        }
    }

    /**
     * 各数据源默认端口
     */
    public int defaultPort(String type) {
        switch (type.toLowerCase()) {
            case "mysql": return 3306;
            case "postgresql":
            case "postgres": return 5432;
            case "clickhouse": return 8123;
            case "oracle": return 1521;
            case "hive": return 10000;
            default: return 3306;
        }
    }

    /**
     * 测试数据源连接
     */
    public boolean testConnection(DlDatasource ds) {
        try (Connection conn = getConnection(ds)) {
            return conn.isValid(5);
        } catch (Exception e) {
            log.warn("数据源连接测试失败: {} - {}", ds.getName(), e.getMessage());
            return false;
        }
    }
}