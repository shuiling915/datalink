package com.datalink.controller;

import com.datalink.common.Constants;
import com.datalink.exception.ResourceNotFoundException;
import com.datalink.model.ColumnInfo;
import com.datalink.model.DlDatasource;
import com.datalink.model.R;
import com.datalink.mapper.DlDatasourceMapper;
import com.datalink.service.DatasourceConnectionFactory;
import com.datalink.service.MetadataService;
import com.datalink.util.CryptoUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据源管理 Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/datasource")
@Tag(name = "数据源管理", description = "数据源的增删改查、连接测试、元数据浏览")
@RequiredArgsConstructor
public class DatasourceController {

    private final DlDatasourceMapper datasourceMapper;
    private final MetadataService metadataService;
    private final DatasourceConnectionFactory connectionFactory;

    @Value("${datalink.crypto.key}")
    private String cryptoKey;

    /**
     * 获取数据源列表（密码已脱敏）
     *
     * @return 数据源列表
     */
    @Operation(summary = "数据源列表")
    @GetMapping("/list")
    public R<List<DlDatasource>> list() {
        List<DlDatasource> result = datasourceMapper.selectList(null);
        result.forEach(ds -> ds.setPassword(Constants.PASSWORD_MASK));
        return R.ok(result);
    }

    /**
     * 查询数据源详情（密码已解密）
     *
     * @param id 数据源 ID
     * @return 数据源详情
     */
    @Operation(summary = "查询数据源详情")
    @GetMapping("/{id}")
    public R<DlDatasource> getById(@PathVariable Long id) {
        DlDatasource ds = datasourceMapper.selectById(id);
        if (ds == null) {
            throw new ResourceNotFoundException("数据源");
        }
        // 前端不需要看到真实密码，只需知道是否已设置
        ds.setPassword(Constants.PASSWORD_MASK);
        return R.ok(ds);
    }

    /**
     * 保存数据源（新增或更新），密码自动加密存储
     *
     * @param ds 数据源对象
     * @return 保存后的数据源（密码已脱敏）
     */
    @Operation(summary = "保存数据源")
    @PostMapping("/save")
    public R<DlDatasource> save(@RequestBody DlDatasource ds) {
        if (ds.getId() != null) {
            ds.setUpdatedAt(LocalDateTime.now());
            if (Constants.PASSWORD_MASK.equals(ds.getPassword())) {
                // 前端未修改密码，保留数据库中已加密的密码
                DlDatasource old = datasourceMapper.selectById(ds.getId());
                if (old != null) {
                    ds.setPassword(old.getPassword());
                }
            } else {
                // 前端提交了新密码，加密后存库
                ds.setPassword(CryptoUtil.encrypt(ds.getPassword(), cryptoKey));
            }
            datasourceMapper.updateById(ds);
        } else {
            ds.setCreatedAt(LocalDateTime.now());
            ds.setUpdatedAt(LocalDateTime.now());
            ds.setStatus(1);
            ds.setPassword(CryptoUtil.encrypt(ds.getPassword(), cryptoKey));
            datasourceMapper.insert(ds);
        }
        ds.setPassword(Constants.PASSWORD_MASK);
        return R.ok(ds);
    }

    /**
     * 删除数据源
     *
     * @param id 数据源 ID
     * @return 操作结果
     */
    @Operation(summary = "删除数据源")
    @DeleteMapping("/{id}")
    public R<String> delete(@PathVariable Long id) {
        datasourceMapper.deleteById(id);
        return R.ok("删除成功");
    }

    /**
     * 测试数据源连接（支持多数据源类型）
     */
    @Operation(summary = "测试数据源连接")
    @PostMapping("/test")
    public R<String> testConnection(@RequestBody DlDatasource ds) {
        try {
            if (ds.getId() != null) {
                DlDatasource old = datasourceMapper.selectById(ds.getId());
                if (old != null) {
                    if (ds.getType() == null || ds.getType().isEmpty()) ds.setType(old.getType());
                    if (ds.getHost() == null || ds.getHost().isEmpty()) ds.setHost(old.getHost());
                    if (ds.getPort() == null) ds.setPort(old.getPort());
                    if (ds.getDatabaseName() == null || ds.getDatabaseName().isEmpty()) ds.setDatabaseName(old.getDatabaseName());
                    if (ds.getUsername() == null || ds.getUsername().isEmpty()) ds.setUsername(old.getUsername());
                    if (ds.getExtraParams() == null) ds.setExtraParams(old.getExtraParams());
                    if (ds.getPassword() == null || ds.getPassword().isEmpty()
                            || Constants.PASSWORD_MASK.equals(ds.getPassword())) {
                        ds.setPassword(CryptoUtil.decryptSafe(old.getPassword(), cryptoKey));
                    }
                }
            }
            boolean ok = connectionFactory.testConnection(ds);
            return ok ? R.ok("连接成功") : R.fail("连接失败，请检查数据源配置");
        } catch (Exception e) {
            log.error("测试数据源连接失败", e);
            return R.fail("连接失败: " + e.getMessage());
        }
    }

    /**
     * 获取支持的数据源类型列表
     */
    @Operation(summary = "支持的数据源类型")
    @GetMapping("/types")
    public R<List<Map<String, Object>>> types() {
        List<Map<String, Object>> types = List.of(
                typeInfo("mysql", "MySQL", 3306),
                typeInfo("postgresql", "PostgreSQL", 5432),
                typeInfo("clickhouse", "ClickHouse", 8123),
                typeInfo("oracle", "Oracle", 1521),
                typeInfo("hive", "Hive", 10000)
        );
        return R.ok(types);
    }

    private Map<String, Object> typeInfo(String type, String name, int defaultPort) {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("type", type);
        info.put("name", name);
        info.put("defaultPort", defaultPort);
        return info;
    }

    /**
     * 获取指定数据源下的数据库列表
     */
    @Operation(summary = "获取数据库列表")
    @GetMapping("/{id}/databases")
    public R<List<String>> databases(@PathVariable Long id) {
        try {
            DlDatasource ds = requireDatasource(id);
            return R.ok(metadataService.getDatabasesByConnection(ds.getHost(), ds.getPort(), ds.getUsername(), ds.getPassword()));
        } catch (SQLException e) {
            log.error("获取数据库列表失败, datasourceId={}", id, e);
            return R.fail("获取数据库列表失败");
        }
    }

    /**
     * 获取指定数据源指定库的表列表
     */
    @Operation(summary = "获取表列表")
    @GetMapping("/{id}/tables")
    public R<List<String>> tables(@PathVariable Long id, @RequestParam String db) {
        try {
            DlDatasource ds = requireDatasource(id);
            return R.ok(metadataService.getTablesByConnection(ds.getHost(), ds.getPort(), ds.getUsername(), ds.getPassword(), db));
        } catch (SQLException e) {
            log.error("获取表列表失败, datasourceId={}, db={}", id, db, e);
            return R.fail("获取表列表失败");
        }
    }

    /**
     * 获取指定数据源指定库表的字段列表
     */
    @Operation(summary = "获取字段列表")
    @GetMapping("/{id}/columns")
    public R<List<ColumnInfo>> columns(@PathVariable Long id, @RequestParam String db, @RequestParam String table) {
        try {
            DlDatasource ds = requireDatasource(id);
            return R.ok(metadataService.getColumnsByConnection(ds.getHost(), ds.getPort(), ds.getUsername(), ds.getPassword(), db, table));
        } catch (SQLException e) {
            log.error("获取字段列表失败, datasourceId={}, db={}, table={}", id, db, table, e);
            return R.fail("获取字段列表失败");
        }
    }

    private DlDatasource requireDatasource(Long id) {
        DlDatasource ds = datasourceMapper.selectById(id);
        if (ds == null) {
            throw new ResourceNotFoundException("数据源");
        }
        ds.setPassword(CryptoUtil.decryptSafe(ds.getPassword(), cryptoKey));
        return ds;
    }

}