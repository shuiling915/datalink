package com.datalink.service;

import com.datalink.mapper.DlDatasourceMapper;
import com.datalink.model.DlDatasource;
import com.datalink.util.CryptoUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class DataPreviewService {

    private final DlDatasourceMapper datasourceMapper;

    @Value("${datalink.crypto.key}")
    private String cryptoKey;

    public Map<String, Object> previewTable(Long datasourceId, String database, String table,
                                             List<String> columns, String whereClause,
                                             int limit, int offset) throws Exception {
        DlDatasource ds = datasourceMapper.selectById(datasourceId);
        if (ds == null) throw new IllegalArgumentException("数据源不存在");

        validateIdentifier(database, "数据库名");
        validateIdentifier(table, "表名");
        if (columns != null) {
            for (String col : columns) validateIdentifier(col, "列名");
        }
        if (whereClause != null && !whereClause.isEmpty()) {
            validateWhereClause(whereClause);
        }

        String colPart = (columns != null && !columns.isEmpty())
                ? String.join(", ", columns.stream().map(c -> "`" + c + "`").toArray(String[]::new))
                : "*";

        StringBuilder sql = new StringBuilder("SELECT ").append(colPart)
                .append(" FROM `").append(database).append("`.`").append(table).append("`");
        if (whereClause != null && !whereClause.isEmpty()) {
            sql.append(" WHERE ").append(whereClause);
        }
        sql.append(" LIMIT ").append(limit);
        if (offset > 0) sql.append(" OFFSET ").append(offset);

        String password = CryptoUtil.decryptSafe(ds.getPassword(), cryptoKey);
        String url = buildJdbcUrl(ds, database);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("datasource", ds.getName());
        result.put("database", database);
        result.put("table", table);
        result.put("sql", sql.toString());

        long start = System.currentTimeMillis();
        try (Connection conn = DriverManager.getConnection(url, ds.getUsername(), password);
             Statement stmt = conn.createStatement()) {
            stmt.setQueryTimeout(30);
            try (ResultSet rs = stmt.executeQuery(sql.toString())) {
                ResultSetMetaData meta = rs.getMetaData();
                int colCount = meta.getColumnCount();

                List<String> colNames = new ArrayList<>();
                List<Map<String, Object>> colMetas = new ArrayList<>();
                for (int i = 1; i <= colCount; i++) {
                    colNames.add(meta.getColumnLabel(i));
                    Map<String, Object> cm = new LinkedHashMap<>();
                    cm.put("name", meta.getColumnLabel(i));
                    cm.put("type", meta.getColumnTypeName(i));
                    cm.put("precision", meta.getPrecision(i));
                    colMetas.add(cm);
                }

                List<Map<String, Object>> rows = new ArrayList<>();
                int rowCount = 0;
                while (rs.next() && rowCount < limit) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (int i = 1; i <= colCount; i++) {
                        Object val = rs.getObject(i);
                        row.put(colNames.get(i - 1), val);
                    }
                    rows.add(row);
                    rowCount++;
                }

                result.put("columns", colNames);
                result.put("columnMeta", colMetas);
                result.put("rows", rows);
                result.put("rowCount", rowCount);
            }
        }
        result.put("durationMs", System.currentTimeMillis() - start);
        return result;
    }

    public Map<String, Object> getTableStats(Long datasourceId, String database, String table) throws Exception {
        DlDatasource ds = datasourceMapper.selectById(datasourceId);
        if (ds == null) throw new IllegalArgumentException("数据源不存在");

        validateIdentifier(database, "数据库名");
        validateIdentifier(table, "表名");

        String password = CryptoUtil.decryptSafe(ds.getPassword(), cryptoKey);
        String url = buildJdbcUrl(ds, database);

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("database", database);
        stats.put("table", table);

        try (Connection conn = DriverManager.getConnection(url, ds.getUsername(), password);
             Statement stmt = conn.createStatement()) {
            stmt.setQueryTimeout(30);

            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM `" + database + "`.`" + table + "`")) {
                if (rs.next()) stats.put("rowCount", rs.getLong(1));
            }

            List<Map<String, Object>> columns = new ArrayList<>();
            try (ResultSet rs = stmt.executeQuery("DESCRIBE `" + database + "`.`" + table + "`")) {
                while (rs.next()) {
                    Map<String, Object> col = new LinkedHashMap<>();
                    col.put("name", rs.getString("Field"));
                    col.put("type", rs.getString("Type"));
                    col.put("nullable", rs.getString("Null"));
                    col.put("key", rs.getString("Key"));
                    col.put("default", rs.getString("Default"));
                    columns.add(col);
                }
            }
            stats.put("columns", columns);
        }
        return stats;
    }

    private String buildJdbcUrl(DlDatasource ds, String database) {
        return "jdbc:mysql://" + ds.getHost() + ":" + ds.getPort()
                + "/" + database + "?useSSL=false&allowPublicKeyRetrieval=true&connectTimeout=10000";
    }

    private void validateIdentifier(String name, String fieldName) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException(fieldName + "不能为空");
        }
        if (!name.matches("^[a-zA-Z_][a-zA-Z0-9_]*$")) {
            throw new IllegalArgumentException(fieldName + "包含非法字符: " + name);
        }
    }

    private void validateWhereClause(String clause) {
        String upper = clause.toUpperCase();
        if (upper.contains(";") || upper.contains("--") || upper.contains("/*")
                || upper.contains("DELETE") || upper.contains("DROP") || upper.contains("UPDATE")
                || upper.contains("INSERT") || upper.contains("ALTER") || upper.contains("CREATE")) {
            throw new IllegalArgumentException("WHERE条件包含非法操作");
        }
    }
}