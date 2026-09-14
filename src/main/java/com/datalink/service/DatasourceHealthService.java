package com.datalink.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.datalink.mapper.DlDatasourceMapper;
import com.datalink.model.DlDatasource;
import com.datalink.util.CryptoUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class DatasourceHealthService {

    private final DlDatasourceMapper datasourceMapper;

    @Value("${datalink.crypto.key}")
    private String cryptoKey;

    private final Map<Long, Map<String, Object>> healthCache = new ConcurrentHashMap<>();

    @Scheduled(fixedRate = 60000)
    public void checkAllHealth() {
        List<DlDatasource> datasources = datasourceMapper.selectList(null);
        for (DlDatasource ds : datasources) {
            try {
                Map<String, Object> health = checkHealth(ds);
                healthCache.put(ds.getId(), health);
            } catch (Exception e) {
                log.warn("数据源健康检查失败: {} - {}", ds.getName(), e.getMessage());
            }
        }
    }

    public Map<String, Object> checkHealth(DlDatasource ds) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("datasourceId", ds.getId());
        result.put("datasourceName", ds.getName());
        result.put("type", ds.getType());
        result.put("host", ds.getHost());
        result.put("port", ds.getPort());
        result.put("checkedAt", LocalDateTime.now().toString());

        String url = buildJdbcUrl(ds);
        String password = CryptoUtil.decryptSafe(ds.getPassword(), cryptoKey);

        long start = System.currentTimeMillis();
        try (Connection conn = DriverManager.getConnection(url, ds.getUsername(), password);
             Statement stmt = conn.createStatement()) {
            stmt.setQueryTimeout(10);
            stmt.execute("SELECT 1");
            long cost = System.currentTimeMillis() - start;

            result.put("status", "ONLINE");
            result.put("responseTimeMs", cost);

            Map<String, Object> dbInfo = new LinkedHashMap<>();
            try {
                dbInfo.put("productName", conn.getMetaData().getDatabaseProductName());
                dbInfo.put("productVersion", conn.getMetaData().getDatabaseProductVersion());
                dbInfo.put("driverName", conn.getMetaData().getDriverName());
                dbInfo.put("driverVersion", conn.getMetaData().getDriverVersion());
                dbInfo.put("maxConnections", conn.getMetaData().getMaxConnections());
            } catch (Exception ignored) {}
            result.put("dbInfo", dbInfo);

        } catch (Exception e) {
            result.put("status", "OFFLINE");
            result.put("error", e.getMessage());
            result.put("responseTimeMs", System.currentTimeMillis() - start);
        }
        return result;
    }

    public Map<String, Object> checkDatasourceById(Long id) {
        DlDatasource ds = datasourceMapper.selectById(id);
        if (ds == null) return null;
        Map<String, Object> health = checkHealth(ds);
        healthCache.put(id, health);
        return health;
    }

    public List<Map<String, Object>> checkAll() {
        List<DlDatasource> datasources = datasourceMapper.selectList(null);
        List<Map<String, Object>> results = new ArrayList<>();
        for (DlDatasource ds : datasources) {
            results.add(checkHealth(ds));
        }
        return results;
    }

    public Map<String, Object> getCachedHealth(Long id) {
        return healthCache.get(id);
    }

    public List<Map<String, Object>> getAllCachedHealth() {
        return new ArrayList<>(healthCache.values());
    }

    public Map<String, Object> getHealthSummary() {
        Map<String, Object> summary = new LinkedHashMap<>();
        int total = healthCache.size();
        int online = 0, offline = 0;
        long avgResponse = 0;
        for (Map<String, Object> h : healthCache.values()) {
            if ("ONLINE".equals(h.get("status"))) {
                online++;
                avgResponse += (Long) h.getOrDefault("responseTimeMs", 0L);
            } else {
                offline++;
            }
        }
        summary.put("total", total);
        summary.put("online", online);
        summary.put("offline", offline);
        summary.put("avgResponseTimeMs", online > 0 ? avgResponse / online : 0);
        return summary;
    }

    private String buildJdbcUrl(DlDatasource ds) {
        String db = ds.getDatabaseName() != null ? ds.getDatabaseName() : "";
        switch (ds.getType().toLowerCase()) {
            case "mysql":
                return "jdbc:mysql://" + ds.getHost() + ":" + ds.getPort() + "/" + db
                        + "?useSSL=false&allowPublicKeyRetrieval=true&connectTimeout=10000";
            case "postgresql":
                return "jdbc:postgresql://" + ds.getHost() + ":" + ds.getPort() + "/" + db;
            case "oracle":
                return "jdbc:oracle:thin:@" + ds.getHost() + ":" + ds.getPort() + ":" + db;
            case "clickhouse":
                return "jdbc:clickhouse://" + ds.getHost() + ":" + ds.getPort() + "/" + db;
            default:
                return "jdbc:mysql://" + ds.getHost() + ":" + ds.getPort() + "/" + db
                        + "?useSSL=false&connectTimeout=10000";
        }
    }
}