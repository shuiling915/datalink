package com.datalink.config;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.datalink.mapper.DlSystemConfigMapper;
import com.datalink.model.DlSystemConfig;
import com.datalink.util.CryptoUtil;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import lombok.extern.slf4j.Slf4j;

import javax.annotation.PostConstruct;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Hive 连接配置 — 使用 HikariCP 连接池管理 Hive JDBC 连接
 * 支持 Hive 不可用时优雅降级（应用正常启动，Hive 功能不可用）
 * 支持从数据库热加载配置（页面配置后无需重启）
 */
@Slf4j
@Configuration
public class HiveConfig {

    @Value("${hive.url:}")
    private String envHiveUrl;

    @Value("${hive.username:}")
    private String envHiveUsername;

    @Value("${hive.password:}")
    private String envHivePassword;

    @Value("${datalink.crypto.key:}")
    private String cryptoKey;

    @Autowired(required = false)
    private DlSystemConfigMapper systemConfigMapper;

    private HikariDataSource hiveDataSource;
    private boolean hiveAvailable = false;
    private String currentUrl = "";

    @PostConstruct
    public void init() {
        // 先尝试从 DB 加载，失败则用环境变量
        String url = getDbConfig("hive.url");
        String username = getDbConfig("hive.username");
        String password = getDbConfigDecrypt("hive.password");

        if (url == null || url.isEmpty()) {
            url = envHiveUrl;
            username = envHiveUsername;
            password = envHivePassword;
        }

        initDataSource(url, username, password);
    }

    /**
     * 从数据库重新加载配置（页面保存后调用）
     */
    public void reloadFromDb() {
        String url = getDbConfig("hive.url");
        String username = getDbConfig("hive.username");
        String password = getDbConfigDecrypt("hive.password");

        // DB 没配置则用环境变量
        if (url == null || url.isEmpty()) {
            url = envHiveUrl;
            username = envHiveUsername;
            password = envHivePassword;
        }

        // 关闭旧连接池
        if (hiveDataSource != null) {
            try {
                hiveDataSource.close();
                log.info("旧 Hive 连接池已关闭");
            } catch (Exception e) {
                log.warn("关闭旧连接池失败: {}", e.getMessage());
            }
            hiveDataSource = null;
            hiveAvailable = false;
        }

        initDataSource(url, username, password);
    }

    private void initDataSource(String url, String username, String password) {
        currentUrl = url != null ? url : "";

        if (url == null || url.isEmpty()) {
            log.warn("Hive URL 未配置，Hive 功能不可用");
            return;
        }

        try {
            Class.forName("org.apache.hive.jdbc.HiveDriver");
        } catch (ClassNotFoundException e) {
            log.warn("Hive JDBC Driver 未找到，Hive 功能不可用");
            return;
        }

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setUsername(username != null ? username : "");
        config.setPassword(password != null ? password : "");
        config.setDriverClassName("org.apache.hive.jdbc.HiveDriver");
        config.setMaximumPoolSize(5);
        config.setMinimumIdle(0);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setPoolName("HiveHikariPool");
        config.setConnectionTestQuery("SELECT 1");
        config.setInitializationFailTimeout(-1);

        try {
            hiveDataSource = new HikariDataSource(config);
            hiveAvailable = true;
            log.info("Hive 连接池初始化成功: {}", url);
        } catch (Exception e) {
            log.warn("Hive 连接池初始化失败: {}", e.getMessage());
        }
    }

    public boolean isHiveAvailable() {
        return hiveAvailable && hiveDataSource != null;
    }

    public String getCurrentUrl() {
        return currentUrl;
    }

    public Connection getConnection() throws SQLException {
        if (hiveDataSource == null) {
            throw new SQLException("Hive 未连接。请在系统管理中配置 Hive 连接。");
        }
        return hiveDataSource.getConnection();
    }

    public Connection getRawConnection() throws SQLException {
        if (currentUrl == null || currentUrl.isEmpty()) {
            throw new SQLException("Hive 未配置。");
        }
        String username = getDbConfig("hive.username");
        String password = getDbConfigDecrypt("hive.password");
        if (username == null) username = envHiveUsername;
        if (password == null) password = envHivePassword;
        return java.sql.DriverManager.getConnection(currentUrl, username, password);
    }

    private String getDbConfig(String key) {
        if (systemConfigMapper == null) return null;
        try {
            DlSystemConfig cfg = systemConfigMapper.selectById(key);
            return cfg != null ? cfg.getConfigValue() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private String getDbConfigDecrypt(String key) {
        String val = getDbConfig(key);
        if (val != null && !val.isEmpty() && cryptoKey != null && !cryptoKey.isEmpty()) {
            String decrypted = CryptoUtil.decrypt(val, cryptoKey);
            return decrypted != null ? decrypted : val;
        }
        return val;
    }
}
