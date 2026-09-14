package com.datalink.service;

import com.datalink.mapper.DlDatasourceMapper;
import com.datalink.model.ColumnInfo;
import com.datalink.model.DlAsset;
import com.datalink.model.DlDatasource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MetadataService {

    private final DataSource dataSource;
    private final DlDatasourceMapper datasourceMapper;
    private final DatasourceConnectionFactory connectionFactory;
    private final AssetService assetService;

    private static final String SQL_DATABASES =
            "SELECT SCHEMA_NAME FROM information_schema.SCHEMATA "
            + "WHERE SCHEMA_NAME NOT IN ('information_schema','performance_schema','mysql','sys') "
            + "ORDER BY SCHEMA_NAME";

    private static final String SQL_TABLES =
            "SELECT TABLE_NAME FROM information_schema.TABLES "
            + "WHERE TABLE_SCHEMA = ? AND TABLE_TYPE = 'BASE TABLE' "
            + "ORDER BY TABLE_NAME";

    private static final String SQL_COLUMNS =
            "SELECT COLUMN_NAME, COLUMN_TYPE, COLUMN_COMMENT, COLUMN_KEY, IS_NULLABLE, EXTRA "
            + "FROM information_schema.COLUMNS "
            + "WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ? "
            + "ORDER BY ORDINAL_POSITION";

    /**
     * 获取默认数据源的所有数据库（排除系统库）
     */
    public List<String> getDatabases() throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            return queryDatabases(conn);
        }
    }

    /**
     * 获取外部连接的所有数据库
     */
    public List<String> getDatabasesByConnection(String host, int port, String username, String password) throws SQLException {
        try (Connection conn = getExternalConnection(host, port, username, password)) {
            return queryDatabases(conn);
        }
    }

    /**
     * 获取默认数据源指定库的所有表
     */
    public List<String> getTables(String db) throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            return queryTables(conn, db);
        }
    }

    /**
     * 获取外部连接指定库的所有表
     */
    public List<String> getTablesByConnection(String host, int port, String username, String password, String db) throws SQLException {
        try (Connection conn = getExternalConnection(host, port, username, password)) {
            return queryTables(conn, db);
        }
    }

    /**
     * 获取默认数据源指定表的所有字段信息
     */
    public List<ColumnInfo> getColumns(String db, String table) throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            return queryColumns(conn, db, table);
        }
    }

    /**
     * 获取外部连接指定表的所有字段信息
     */
    public List<ColumnInfo> getColumnsByConnection(String host, int port, String username, String password, String db, String table) throws SQLException {
        try (Connection conn = getExternalConnection(host, port, username, password)) {
            return queryColumns(conn, db, table);
        }
    }

    // ========== 核心查询逻辑（消除重复） ==========

    private List<String> queryDatabases(Connection conn) throws SQLException {
        List<String> list = new ArrayList<>();
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(SQL_DATABASES)) {
            while (rs.next()) {
                list.add(rs.getString(1));
            }
        }
        return list;
    }

    private List<String> queryTables(Connection conn, String db) throws SQLException {
        List<String> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(SQL_TABLES)) {
            ps.setString(1, db);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(rs.getString(1));
                }
            }
        }
        return list;
    }

    private List<ColumnInfo> queryColumns(Connection conn, String db, String table) throws SQLException {
        List<ColumnInfo> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(SQL_COLUMNS)) {
            ps.setString(1, db);
            ps.setString(2, table);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ColumnInfo col = new ColumnInfo();
                    col.setName(rs.getString("COLUMN_NAME"));
                    col.setType(rs.getString("COLUMN_TYPE"));
                    col.setComment(rs.getString("COLUMN_COMMENT"));
                    col.setKey(rs.getString("COLUMN_KEY"));
                    col.setNullable(rs.getString("IS_NULLABLE"));
                    col.setExtra(rs.getString("EXTRA"));
                    col.setHiveType("string");
                    list.add(col);
                }
            }
        }
        return list;
    }

    private Connection getExternalConnection(String host, int port, String username, String password) throws SQLException {
        String url = "jdbc:mysql://" + host + ":" + port
                + "/?useUnicode=true&characterEncoding=UTF-8&useSSL=false&allowPublicKeyRetrieval=true";
        return DriverManager.getConnection(url, username, password);
    }

    // ==================== 按配置数据源采集 ====================

    /**
     * 获取指定数据源的所有数据库
     */
    public List<String> getDatabasesByDatasource(Long datasourceId) throws SQLException {
        DlDatasource ds = datasourceMapper.selectById(datasourceId);
        if (ds == null) throw new IllegalArgumentException("数据源不存在");
        try (Connection conn = connectionFactory.getConnection(ds)) {
            return queryDatabases(conn);
        }
    }

    /**
     * 获取指定数据源指定库的所有表
     */
    public List<String> getTablesByDatasource(Long datasourceId, String db) throws SQLException {
        DlDatasource ds = datasourceMapper.selectById(datasourceId);
        if (ds == null) throw new IllegalArgumentException("数据源不存在");
        try (Connection conn = connectionFactory.getConnection(ds)) {
            return queryTables(conn, db);
        }
    }

    /**
     * 获取指定数据源指定表的字段信息
     */
    public List<ColumnInfo> getColumnsByDatasource(Long datasourceId, String db, String table) throws SQLException {
        DlDatasource ds = datasourceMapper.selectById(datasourceId);
        if (ds == null) throw new IllegalArgumentException("数据源不存在");
        try (Connection conn = connectionFactory.getConnection(ds)) {
            return queryColumns(conn, db, table);
        }
    }

    /**
     * 自动采集指定数据源的所有表结构，并同步到数据资产目录
     * @param datasourceId 数据源ID
     * @param dbName 数据库名（为空则采集所有库）
     * @param operator 操作人
     * @return 采集结果统计
     */
    public java.util.Map<String, Object> syncToAssets(Long datasourceId, String dbName, String operator) throws SQLException {
        DlDatasource ds = datasourceMapper.selectById(datasourceId);
        if (ds == null) throw new IllegalArgumentException("数据源不存在");

        List<String> databases;
        if (dbName != null && !dbName.isEmpty()) {
            databases = List.of(dbName);
        } else {
            try (Connection conn = connectionFactory.getConnection(ds)) {
                databases = queryDatabases(conn);
            }
        }

        int tableCount = 0;
        int newCount = 0;
        int updateCount = 0;
        List<String> scannedTables = new ArrayList<>();

        for (String db : databases) {
            List<String> tables;
            try (Connection conn = connectionFactory.getConnection(ds)) {
                tables = queryTables(conn, db);
            }
            for (String table : tables) {
                tableCount++;
                scannedTables.add(db + "." + table);

                try {
                    // 获取表注释和行数
                    String tableComment = getTableComment(ds, db, table);
                    long rowCount = getRowCount(ds, db, table);

                    // 获取字段信息
                    List<ColumnInfo> columns;
                    try (Connection conn = connectionFactory.getConnection(ds)) {
                        columns = queryColumns(conn, db, table);
                    }
                    String columnDesc = buildColumnDescription(columns);

                    // 保存到资产目录（已存在则更新）
                    DlAsset asset = new DlAsset();
                    asset.setDatasourceId(datasourceId);
                    asset.setTableName(db + "." + table);
                    asset.setAssetName(table);
                    asset.setDescription((tableComment != null ? tableComment + " " : "")
                            + "【自动采集】字段: " + columns.size() + "个, 行数: " + rowCount);
                    asset.setAssetLevel(1);
                    asset.setOwner(operator);
                    asset.setBusinessDomain(db);
                    asset.setStatus(1);
                    asset.setAccessCount(0L);

                    // 检查是否已存在
                    DlAsset existing = assetService.findByDatasourceAndTable(datasourceId, db + "." + table);
                    if (existing != null) {
                        asset.setId(existing.getId());
                        asset.setAccessCount(existing.getAccessCount());
                        updateCount++;
                    } else {
                        newCount++;
                    }
                    assetService.save(asset, operator);
                } catch (Exception e) {
                    log.warn("采集表 {}.{} 元数据失败: {}", db, table, e.getMessage());
                }
            }
        }

        java.util.Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("datasourceId", datasourceId);
        result.put("databases", databases);
        result.put("tableCount", tableCount);
        result.put("newAssets", newCount);
        result.put("updatedAssets", updateCount);
        result.put("scannedTables", scannedTables);
        return result;
    }

    private String getTableComment(DlDatasource ds, String db, String table) {
        String sql = "SELECT TABLE_COMMENT FROM information_schema.TABLES WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ?";
        try (Connection conn = connectionFactory.getConnection(ds);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, db);
            ps.setString(2, table);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString(1);
            }
        } catch (Exception e) {
            log.warn("获取表注释失败 {}.{}: {}", db, table, e.getMessage());
        }
        return null;
    }

    private long getRowCount(DlDatasource ds, String db, String table) {
        String sql = "SELECT TABLE_ROWS FROM information_schema.TABLES WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ?";
        try (Connection conn = connectionFactory.getConnection(ds);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, db);
            ps.setString(2, table);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getLong(1);
            }
        } catch (Exception e) {
            log.warn("获取行数失败 {}.{}: {}", db, table, e.getMessage());
        }
        return 0;
    }

    private String buildColumnDescription(List<ColumnInfo> columns) {
        StringBuilder sb = new StringBuilder();
        for (ColumnInfo col : columns) {
            sb.append(col.getName()).append("(").append(col.getType()).append(")");
            if (col.getComment() != null && !col.getComment().isEmpty()) {
                sb.append(":").append(col.getComment());
            }
            sb.append(", ");
        }
        if (sb.length() > 400) {
            return sb.substring(0, 400) + "...";
        }
        return sb.toString();
    }
}