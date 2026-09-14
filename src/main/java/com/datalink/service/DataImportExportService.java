package com.datalink.service;

import com.alibaba.fastjson.JSON;
import com.datalink.mapper.DlDataImportMapper;
import com.datalink.mapper.DlDatasourceMapper;
import com.datalink.model.DlDataImport;
import com.datalink.model.DlDatasource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;

/**
 * 数据导入导出服务 — CSV 文件导入建表、SQL 结果导出 CSV
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DataImportExportService {

    private final DlDatasourceMapper datasourceMapper;
    private final DlDataImportMapper dataImportMapper;
    private final DatasourceConnectionFactory connectionFactory;

    /**
     * 导入 CSV 文件到目标数据源（自动建表 + 批量插入）
     *
     * @param file         CSV 文件
     * @param datasourceId 目标数据源ID
     * @param tableName    目标表名
     * @param createTable  是否自动建表
     * @param truncate     导入前是否清空表
     */
    public DlDataImport importCsv(MultipartFile file, Long datasourceId, String tableName,
                                  boolean createTable, boolean truncate, String operator) throws Exception {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("上传文件不能为空");
        }
        if (tableName == null || tableName.trim().isEmpty()) {
            throw new IllegalArgumentException("目标表名不能为空");
        }

        DlDatasource ds = datasourceMapper.selectById(datasourceId);
        if (ds == null) {
            throw new IllegalArgumentException("数据源不存在");
        }

        String safeTableName = sanitizeTableName(tableName);
        List<String[]> rows = parseCsv(file);
        if (rows.isEmpty()) {
            throw new IllegalArgumentException("CSV 文件为空");
        }

        String[] headers = rows.get(0);
        List<String[]> dataRows = rows.subList(1, rows.size());

        DlDataImport record = new DlDataImport();
        record.setFileName(file.getOriginalFilename());
        record.setFileSize(file.getSize());
        record.setDatasourceId(datasourceId);
        record.setDatabaseName(ds.getDatabaseName());
        record.setTableName(safeTableName);
        record.setColumnCount(headers.length);
        record.setCreatedBy(operator);

        try (Connection conn = connectionFactory.getConnection(ds)) {
            if (createTable) {
                autoCreateTable(conn, safeTableName, headers, dataRows, ds.getType());
            }
            if (truncate) {
                try (Statement stmt = conn.createStatement()) {
                    stmt.execute("TRUNCATE TABLE " + safeTableName);
                }
            }
            int imported = batchInsert(conn, safeTableName, headers, dataRows);
            record.setRowCount(imported);
            record.setStatus("SUCCESS");
            record.setColumnsInfo(JSON.toJSONString(headers));
        } catch (Exception e) {
            log.error("CSV 导入失败", e);
            record.setStatus("FAILED");
            record.setErrorMessage(e.getMessage() != null && e.getMessage().length() > 1000
                    ? e.getMessage().substring(0, 1000) : e.getMessage());
            dataImportMapper.insert(record);
            throw e;
        }

        dataImportMapper.insert(record);
        return record;
    }

    /**
     * 解析 CSV 文件，返回所有行（含表头）
     */
    private List<String[]> parseCsv(MultipartFile file) throws IOException {
        List<String[]> result = new ArrayList<>();
        try (Reader reader = new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8);
             CSVParser parser = CSVFormat.DEFAULT
                     .withFirstRecordAsHeader()
                     .withIgnoreHeaderCase()
                     .withTrim()
                     .parse(reader)) {

            // 获取表头
            List<String> headerNames = parser.getHeaderNames();
            result.add(headerNames.toArray(new String[0]));

            for (CSVRecord record : parser) {
                String[] row = new String[headerNames.size()];
                for (int i = 0; i < headerNames.size(); i++) {
                    row[i] = record.get(i);
                }
                result.add(row);
            }
        }
        return result;
    }

    /**
     * 自动建表（所有列默认 VARCHAR(1024)，根据数据可适当推断整数/浮点）
     */
    private void autoCreateTable(Connection conn, String tableName, String[] headers,
                                 List<String[]> dataRows, String dsType) throws SQLException {
        // 推断列类型
        String[] colTypes = inferColumnTypes(headers, dataRows);

        StringBuilder ddl = new StringBuilder("CREATE TABLE IF NOT EXISTS ").append(tableName).append(" (");
        for (int i = 0; i < headers.length; i++) {
            String colName = "`" + sanitizeColumnName(headers[i]) + "`";
            ddl.append(colName).append(" ").append(colTypes[i]);
            if (i < headers.length - 1) ddl.append(", ");
        }
        // MySQL 需要 ENGINE
        if ("mysql".equalsIgnoreCase(dsType)) {
            ddl.append(") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
        } else {
            ddl.append(")");
        }

        log.info("自动建表 DDL: {}", ddl);
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(ddl.toString());
        }
    }

    /**
     * 根据数据推断列类型：优先 INT → DOUBLE → VARCHAR
     */
    private String[] inferColumnTypes(String[] headers, List<String[]> dataRows) {
        String[] types = new String[headers.length];
        int sampleSize = Math.min(dataRows.size(), 50);

        for (int col = 0; col < headers.length; col++) {
            boolean allInt = true;
            boolean allDouble = true;
            int maxLen = 0;

            for (int row = 0; row < sampleSize; row++) {
                String val = dataRows.get(row)[col];
                if (val == null || val.trim().isEmpty()) continue;
                val = val.trim();
                maxLen = Math.max(maxLen, val.length());
                if (!val.matches("-?\\d+")) allInt = false;
                if (!val.matches("-?\\d+\\.?\\d*")) allDouble = false;
            }

            if (allInt && sampleSize > 0) {
                types[col] = "BIGINT";
            } else if (allDouble && sampleSize > 0) {
                types[col] = "DOUBLE";
            } else {
                int len = Math.max(255, maxLen * 2);
                types[col] = "VARCHAR(" + Math.min(len, 4096) + ")";
            }
        }
        return types;
    }

    /**
     * 批量插入数据
     */
    private int batchInsert(Connection conn, String tableName, String[] headers,
                            List<String[]> dataRows) throws SQLException {
        if (dataRows.isEmpty()) return 0;

        StringBuilder placeholders = new StringBuilder();
        for (int i = 0; i < headers.length; i++) {
            placeholders.append(i == 0 ? "?" : ",?");
        }
        String sql = "INSERT INTO " + tableName + " VALUES (" + placeholders + ")";

        int batchSize = 500;
        int total = 0;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < dataRows.size(); i++) {
                String[] row = dataRows.get(i);
                for (int j = 0; j < headers.length; j++) {
                    ps.setString(j + 1, j < row.length ? row[j] : null);
                }
                ps.addBatch();
                if ((i + 1) % batchSize == 0) {
                    ps.executeBatch();
                    total += batchSize;
                }
            }
            ps.executeBatch();
            total += dataRows.size() % batchSize;
        }
        return total;
    }

    /**
     * 将 SQL 查询结果导出为 CSV 字符串
     */
    public String exportToCsv(Long datasourceId, String sql) throws Exception {
        DlDatasource ds = datasourceMapper.selectById(datasourceId);
        if (ds == null) {
            throw new IllegalArgumentException("数据源不存在");
        }

        StringBuilder csv = new StringBuilder();
        try (Connection conn = connectionFactory.getConnection(ds);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            ResultSetMetaData meta = rs.getMetaData();
            int colCount = meta.getColumnCount();

            // 表头
            for (int i = 1; i <= colCount; i++) {
                if (i > 1) csv.append(",");
                csv.append(escapeCsv(meta.getColumnName(i)));
            }
            csv.append("\n");

            // 数据行
            while (rs.next()) {
                for (int i = 1; i <= colCount; i++) {
                    if (i > 1) csv.append(",");
                    Object val = rs.getObject(i);
                    csv.append(escapeCsv(val != null ? val.toString() : ""));
                }
                csv.append("\n");
            }
        }
        return csv.toString();
    }

    /**
     * CSV 字段转义
     */
    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private String sanitizeTableName(String name) {
        return name.replaceAll("[^a-zA-Z0-9_]", "_");
    }

    private String sanitizeColumnName(String name) {
        return name.replaceAll("[^a-zA-Z0-9_]", "_");
    }

    /**
     * 导入历史列表
     */
    public List<DlDataImport> listHistory(String operator, int limit) {
        return dataImportMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<DlDataImport>()
                        .eq(operator != null, "created_by", operator)
                        .orderByDesc("created_at")
                        .last("LIMIT " + Math.min(limit, 200)));
    }
}