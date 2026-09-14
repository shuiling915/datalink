package com.datalink.controller;

import com.datalink.config.HiveConfig;
import com.datalink.mapper.DlSystemConfigMapper;
import com.datalink.model.DlSystemConfig;
import com.datalink.model.R;
import com.datalink.util.CryptoUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.sql.Connection;
import java.sql.DriverManager;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@RestController
@RequestMapping("/api/system/config")
@RequiredArgsConstructor
public class SystemConfigController {

    private final DlSystemConfigMapper configMapper;
    private final HiveConfig hiveConfig;

    @Value("${datalink.crypto.key:}")
    private String cryptoKey;

    /**
     * 按前缀读取配置组（如 hive.*, hdfs.*, datax.*）
     */
    @GetMapping("/{group}")
    public R<Map<String, String>> getGroup(@PathVariable String group) {
        List<DlSystemConfig> all = configMapper.selectList(null);
        Map<String, String> result = new LinkedHashMap<>();
        String prefix = group + ".";
        for (DlSystemConfig cfg : all) {
            if (cfg.getConfigKey().startsWith(prefix)) {
                String val = cfg.getConfigValue();
                // 密码类字段不返回明文，只返回占位
                if (cfg.getConfigKey().endsWith(".password") && val != null && !val.isEmpty()) {
                    result.put(cfg.getConfigKey(), "******");
                } else {
                    result.put(cfg.getConfigKey(), val);
                }
            }
        }
        return R.ok(result);
    }

    /**
     * 批量保存配置
     */
    @PostMapping
    public R<Void> save(@RequestBody Map<String, String> configs) {
        for (Map.Entry<String, String> entry : configs.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();

            // 密码占位符不覆盖
            if (key.endsWith(".password") && "******".equals(value)) {
                continue;
            }

            // 密码加密存储
            if (key.endsWith(".password") && value != null && !value.isEmpty()
                    && cryptoKey != null && !cryptoKey.isEmpty()) {
                value = CryptoUtil.encrypt(value, cryptoKey);
            }

            DlSystemConfig existing = configMapper.selectById(key);
            if (existing != null) {
                existing.setConfigValue(value);
                existing.setUpdatedAt(LocalDateTime.now());
                configMapper.updateById(existing);
            } else {
                DlSystemConfig cfg = new DlSystemConfig();
                cfg.setConfigKey(key);
                cfg.setConfigValue(value);
                cfg.setUpdatedAt(LocalDateTime.now());
                configMapper.insert(cfg);
            }
        }

        // 保存后热加载 Hive 配置
        hiveConfig.reloadFromDb();

        return R.ok();
    }

    /**
     * 测试 Hive 连接
     */
    @PostMapping("/test-hive")
    public R<String> testHive(@RequestBody Map<String, String> params) {
        String host = params.getOrDefault("host", "127.0.0.1");
        String port = params.getOrDefault("port", "10000");
        String authMode = params.getOrDefault("authMode", "noSasl");
        String database = params.getOrDefault("database", "default");
        String username = params.getOrDefault("username", "");
        String password = params.getOrDefault("password", "");

        String url = "jdbc:hive2://" + host + ":" + port + "/" + database;
        if ("noSasl".equalsIgnoreCase(authMode)) {
            url += ";auth=noSasl";
        }

        try {
            Class.forName("org.apache.hive.jdbc.HiveDriver");
        } catch (ClassNotFoundException e) {
            return R.fail("Hive JDBC Driver 未找到");
        }

        long start = System.currentTimeMillis();
        try (Connection conn = DriverManager.getConnection(url, username, password)) {
            conn.createStatement().execute("SELECT 1");
            long cost = System.currentTimeMillis() - start;
            return R.ok("连接成功（" + cost + "ms）");
        } catch (Exception e) {
            return R.fail("连接失败: " + e.getMessage());
        }
    }

    /**
     * 测试 HDFS 连接（通过 Socket 测试 NameNode 端口连通性）
     */
    @PostMapping("/test-hdfs")
    public R<String> testHdfs(@RequestBody Map<String, String> params) {
        String defaultFs = params.getOrDefault("defaultFs", "hdfs://localhost:9000");

        // 从 hdfs://host:port 解析出 host 和 port
        String host;
        int port;
        try {
            java.net.URI uri = new java.net.URI(defaultFs);
            host = uri.getHost();
            port = uri.getPort() > 0 ? uri.getPort() : 9000;
        } catch (Exception e) {
            return R.fail("地址格式错误，应为 hdfs://host:port");
        }

        long start = System.currentTimeMillis();
        try (java.net.Socket socket = new java.net.Socket()) {
            socket.connect(new java.net.InetSocketAddress(host, port), 5000);
            long cost = System.currentTimeMillis() - start;
            return R.ok("HDFS NameNode 连接成功（" + cost + "ms）");
        } catch (Exception e) {
            return R.fail("HDFS 连接失败: " + host + ":" + port + " - " + e.getMessage());
        }
    }

    /**
     * 获取当前 Hive 连接状态
     */
    @GetMapping("/hive-status")
    public R<Map<String, Object>> hiveStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("available", hiveConfig.isHiveAvailable());
        status.put("url", hiveConfig.getCurrentUrl());
        return R.ok(status);
    }
}
