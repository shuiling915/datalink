package com.datalink.service;

import com.datalink.mapper.DlDatasourceMapper;
import com.datalink.model.DlDatasource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.util.*;

/**
 * 数据一致性校验服务 — 源端 vs 目标端数据对账
 * 支持：行数对比、主键集合对比、抽样数据对比
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DataComparisonService {

    private final DatasourceConnectionFactory connectionFactory;
    private final DlDatasourceMapper datasourceMapper;

    /**
     * 执行数据一致性校验
     */
    public Map<String, Object> compare(Long sourceDsId, String sourceTable,
                                       Long targetDsId, String targetTable,
                                       String primaryKey, String compareMode) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("sourceDsId", sourceDsId);
        result.put("sourceTable", sourceTable);
        result.put("targetDsId", targetDsId);
        result.put("targetTable", targetTable);
        result.put("primaryKey", primaryKey);
        result.put("compareMode", compareMode);
        result.put("startTime", System.currentTimeMillis());

        try {
            DlDatasource sourceDs = getDatasource(sourceDsId);
            DlDatasource targetDs = getDatasource(targetDsId);

            switch (compareMode != null ? compareMode : "row_count") {
                case "row_count":
                    result.put("rowCount", compareRowCount(sourceDs, sourceTable, targetDs, targetTable));
                    break;
                case "primary_key":
                    result.put("primaryKeyDiff", comparePrimaryKeys(sourceDs, sourceTable, targetDs, targetTable, primaryKey));
                    break;
                case "sample":
                    result.put("sampleDiff", compareSampleData(sourceDs, sourceTable, targetDs, targetTable, primaryKey, 1000));
                    break;
                case "full":
                    result.put("rowCount", compareRowCount(sourceDs, sourceTable, targetDs, targetTable));
                    result.put("primaryKeyDiff", comparePrimaryKeys(sourceDs, sourceTable, targetDs, targetTable, primaryKey));
                    break;
                default:
                    throw new IllegalArgumentException("不支持的对比模式: " + compareMode);
            }
            result.put("status", "success");
        } catch (Exception e) {
            log.error("数据一致性校验失败", e);
            result.put("status", "error");
            result.put("errorMsg", e.getMessage());
        }

        result.put("endTime", System.currentTimeMillis());
        result.put("durationMs", (Long) result.get("endTime") - (Long) result.get("startTime"));
        return result;
    }

    /**
     * 行数对比
     */
    private Map<String, Object> compareRowCount(DlDatasource sourceDs, String sourceTable,
                                                DlDatasource targetDs, String targetTable) throws Exception {
        Map<String, Object> rowCountResult = new LinkedHashMap<>();

        long sourceCount = queryCount(sourceDs, sourceTable);
        long targetCount = queryCount(targetDs, targetTable);

        rowCountResult.put("sourceCount", sourceCount);
        rowCountResult.put("targetCount", targetCount);
        rowCountResult.put("diff", sourceCount - targetCount);
        rowCountResult.put("matched", sourceCount == targetCount);
        rowCountResult.put("diffRate", sourceCount > 0
                ? String.format("%.4f", Math.abs(sourceCount - targetCount) * 100.0 / sourceCount) + "%"
                : "0.0000%");
        return rowCountResult;
    }

    /**
     * 主键集合对比
     */
    private Map<String, Object> comparePrimaryKeys(DlDatasource sourceDs, String sourceTable,
                                                   DlDatasource targetDs, String targetTable,
                                                   String primaryKey) throws Exception {
        Map<String, Object> pkResult = new LinkedHashMap<>();

        Set<Object> sourceKeys = queryPrimaryKeys(sourceDs, sourceTable, primaryKey);
        Set<Object> targetKeys = queryPrimaryKeys(targetDs, targetTable, primaryKey);

        Set<Object> onlyInSource = new LinkedHashSet<>(sourceKeys);
        onlyInSource.removeAll(targetKeys);

        Set<Object> onlyInTarget = new LinkedHashSet<>(targetKeys);
        onlyInTarget.removeAll(sourceKeys);

        Set<Object> commonKeys = new LinkedHashSet<>(sourceKeys);
        commonKeys.retainAll(targetKeys);

        pkResult.put("sourceKeyCount", sourceKeys.size());
        pkResult.put("targetKeyCount", targetKeys.size());
        pkResult.put("commonKeyCount", commonKeys.size());
        pkResult.put("onlyInSourceCount", onlyInSource.size());
        pkResult.put("onlyInTargetCount", onlyInTarget.size());
        pkResult.put("matched", onlyInSource.isEmpty() && onlyInTarget.isEmpty());

        // 只返回前 100 条差异主键，避免数据过大
        pkResult.put("onlyInSourceSample", limitSet(onlyInSource, 100));
        pkResult.put("onlyInTargetSample", limitSet(onlyInTarget, 100));
        return pkResult;
    }

    /**
     * 抽样数据对比（对比共同主键的字段值）
     */
    private Map<String, Object> compareSampleData(DlDatasource sourceDs, String sourceTable,
                                                  DlDatasource targetDs, String targetTable,
                                                  String primaryKey, int sampleSize) throws Exception {
        Map<String, Object> sampleResult = new LinkedHashMap<>();

        Set<Object> sourceKeys = queryPrimaryKeys(sourceDs, sourceTable, primaryKey);
        Set<Object> targetKeys = queryPrimaryKeys(targetDs, targetTable, primaryKey);
        Set<Object> commonKeys = new LinkedHashSet<>(sourceKeys);
        commonKeys.retainAll(targetKeys);

        List<Object> sampleKeys = new ArrayList<>(commonKeys);
        if (sampleKeys.size() > sampleSize) {
            Collections.shuffle(sampleKeys);
            sampleKeys = sampleKeys.subList(0, sampleSize);
        }

        List<String> sourceColumns = getColumns(sourceDs, sourceTable);
        List<String> targetColumns = getColumns(targetDs, targetTable);
        Set<String> commonColumns = new LinkedHashSet<>(sourceColumns);
        commonColumns.retainAll(targetColumns);

        int totalRows = sampleKeys.size();
        int matchedRows = 0;
        int mismatchedRows = 0;
        List<Map<String, Object>> mismatchSamples = new ArrayList<>();

        for (Object key : sampleKeys) {
            Map<String, Object> sourceRow = queryRowByKey(sourceDs, sourceTable, primaryKey, key, commonColumns);
            Map<String, Object> targetRow = queryRowByKey(targetDs, targetTable, primaryKey, key, commonColumns);

            boolean rowMatch = true;
            Map<String, Object> diffFields = new LinkedHashMap<>();
            for (String col : commonColumns) {
                if (col.equalsIgnoreCase(primaryKey)) continue;
                Object srcVal = sourceRow.get(col.toLowerCase());
                Object tgtVal = targetRow.get(col.toLowerCase());
                if (!Objects.equals(String.valueOf(srcVal), String.valueOf(tgtVal))) {
                    rowMatch = false;
                    diffFields.put(col, Map.of("source", srcVal, "target", tgtVal));
                }
            }

            if (rowMatch) {
                matchedRows++;
            } else {
                mismatchedRows++;
                if (mismatchSamples.size() < 50) {
                    Map<String, Object> sample = new LinkedHashMap<>();
                    sample.put(primaryKey, key);
                    sample.put("diffFields", diffFields);
                    mismatchSamples.add(sample);
                }
            }
        }

        sampleResult.put("sampleSize", totalRows);
        sampleResult.put("matchedRows", matchedRows);
        sampleResult.put("mismatchedRows", mismatchedRows);
        sampleResult.put("matchRate", totalRows > 0
                ? String.format("%.2f", matchedRows * 100.0 / totalRows) + "%"
                : "0.00%");
        sampleResult.put("mismatchSamples", mismatchSamples);
        return sampleResult;
    }

    private long queryCount(DlDatasource ds, String table) throws Exception {
        validateTable(table);
        try (Connection conn = connectionFactory.getConnection(ds);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM " + table)) {
            return rs.next() ? rs.getLong(1) : 0;
        }
    }

    private Set<Object> queryPrimaryKeys(DlDatasource ds, String table, String pk) throws Exception {
        validateTable(table);
        validateColumn(pk);
        Set<Object> keys = new LinkedHashSet<>();
        try (Connection conn = connectionFactory.getConnection(ds);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT " + pk + " FROM " + table)) {
            while (rs.next()) {
                keys.add(rs.getObject(1));
            }
        }
        return keys;
    }

    private List<String> getColumns(DlDatasource ds, String table) throws Exception {
        List<String> columns = new ArrayList<>();
        try (Connection conn = connectionFactory.getConnection(ds);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM " + table + " WHERE 1=0")) {
            ResultSetMetaData meta = rs.getMetaData();
            for (int i = 1; i <= meta.getColumnCount(); i++) {
                columns.add(meta.getColumnName(i).toLowerCase());
            }
        }
        return columns;
    }

    private Map<String, Object> queryRowByKey(DlDatasource ds, String table, String pk, Object key,
                                              Set<String> columns) throws Exception {
        Map<String, Object> row = new LinkedHashMap<>();
        String colStr = String.join(",", columns);
        String sql = "SELECT " + colStr + " FROM " + table + " WHERE " + pk + " = ?";
        try (Connection conn = connectionFactory.getConnection(ds);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, key);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    ResultSetMetaData meta = rs.getMetaData();
                    for (int i = 1; i <= meta.getColumnCount(); i++) {
                        row.put(meta.getColumnName(i).toLowerCase(), rs.getObject(i));
                    }
                }
            }
        }
        return row;
    }

    private List<Object> limitSet(Set<Object> set, int limit) {
        List<Object> list = new ArrayList<>(set);
        return list.size() > limit ? list.subList(0, limit) : list;
    }

    private void validateTable(String table) {
        if (table == null || !table.matches("^[\\w.]+$")) {
            throw new IllegalArgumentException("非法表名: " + table);
        }
    }

    private void validateColumn(String col) {
        if (col == null || !col.matches("^[\\w]+$")) {
            throw new IllegalArgumentException("非法列名: " + col);
        }
    }

    private DlDatasource getDatasource(Long id) {
        DlDatasource ds = datasourceMapper.selectById(id);
        if (ds == null) {
            throw new IllegalArgumentException("数据源不存在: " + id);
        }
        return ds;
    }
}