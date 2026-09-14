package com.datalink.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.datalink.common.Constants;
import com.datalink.mapper.*;
import com.datalink.model.*;
import com.datalink.model.R;
import com.datalink.service.DataPermissionService;
import com.datalink.service.DatasourceConnectionFactory;
import com.datalink.service.ScriptService;
import com.datalink.service.SqlHistoryService;
import com.datalink.service.TaskDependencyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@RestController
@RequestMapping("/api/data-development")
@Tag(name = "数据开发", description = "SQL编辑器、任务管理、依赖配置、调度管理、双环境发布")
@RequiredArgsConstructor
public class DataDevelopmentController {

    private final DlScriptMapper scriptMapper;
    private final DlSyncTaskMapper syncTaskMapper;
    private final DlScriptFolderMapper folderMapper;
    private final DlTaskDependencyMapper depMapper;
    private final DlTaskExecutionMapper taskExecutionMapper;
    private final DlSchedulerRunMapper schedulerRunMapper;
    private final DlDatasourceMapper datasourceMapper;
    private final DlScriptPublishMapper publishMapper;
    private final ScriptService scriptService;
    private final TaskDependencyService dependencyService;
    private final DataPermissionService dataPermissionService;
    private final DlUserMapper userMapper;
    private final DatasourceConnectionFactory connectionFactory;
    private final SqlHistoryService sqlHistoryService;

    @Value("${datalink.crypto.key}")
    private String cryptoKey;

    // ======================== 任务列表 ========================

    @Operation(summary = "获取所有任务（按分层+环境组织）")
    @GetMapping("/tasks")
    public R<Map<String, Object>> listTasks(
            @RequestParam(required = false) String layer,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false, defaultValue = "dev") String environment) {

        Map<String, Object> result = new LinkedHashMap<>();

        // 获取脚本任务（按环境筛选）
        QueryWrapper<DlScript> scriptQw = new QueryWrapper<>();
        scriptQw.eq("environment", environment);
        if (layer != null && !layer.isEmpty()) {
            scriptQw.eq("task_layer", layer);
        }
        if (keyword != null && !keyword.isEmpty()) {
            scriptQw.and(w -> w.like("script_name", keyword).or().like("description", keyword));
        }
        scriptQw.orderByDesc("updated_at");
        List<DlScript> scripts = scriptMapper.selectList(scriptQw);

        // 获取同步任务
        QueryWrapper<DlSyncTask> syncQw = new QueryWrapper<>();
        if (layer != null && !layer.isEmpty()) {
            syncQw.eq("task_layer", layer);
        }
        if (keyword != null && !keyword.isEmpty()) {
            syncQw.like("task_name", keyword);
        }
        syncQw.orderByDesc("updated_at");
        List<DlSyncTask> syncTasks = syncTaskMapper.selectList(syncQw);

        // 按分层组织
        Map<String, List<Map<String, Object>>> tasksByLayer = new LinkedHashMap<>();
        tasksByLayer.put("ODS", new ArrayList<>());
        tasksByLayer.put("DWD", new ArrayList<>());
        tasksByLayer.put("DWS", new ArrayList<>());
        tasksByLayer.put("ADS", new ArrayList<>());
        tasksByLayer.put("DIM", new ArrayList<>());

        for (DlScript s : scripts) {
            Map<String, Object> task = buildScriptTaskMap(s);
            String taskLayer = s.getTaskLayer() != null ? s.getTaskLayer() : "ODS";
            tasksByLayer.computeIfAbsent(taskLayer, k -> new ArrayList<>()).add(task);
        }

        for (DlSyncTask t : syncTasks) {
            Map<String, Object> task = buildSyncTaskMap(t);
            String taskLayer = t.getTaskLayer() != null ? t.getTaskLayer() : "ODS";
            tasksByLayer.computeIfAbsent(taskLayer, k -> new ArrayList<>()).add(task);
        }

        result.put("tasksByLayer", tasksByLayer);
        result.put("totalScripts", scripts.size());
        result.put("totalSyncTasks", syncTasks.size());
        result.put("environment", environment);

        return R.ok(result);
    }

    private Map<String, Object> buildScriptTaskMap(DlScript s) {
        Map<String, Object> task = new HashMap<>();
        task.put("id", s.getId());
        task.put("name", s.getScriptName());
        task.put("type", "script");
        task.put("scriptType", s.getScriptType());
        task.put("layer", s.getTaskLayer() != null ? s.getTaskLayer() : "ODS");
        task.put("status", s.getTaskStatus() != null ? s.getTaskStatus() : "draft");
        task.put("scheduleStatus", s.getScheduleStatus());
        task.put("scheduleCron", s.getScheduleCron());
        task.put("description", s.getDescription());
        task.put("owner", s.getOwner());
        task.put("databaseName", s.getDatabaseName());
        task.put("updatedAt", s.getUpdatedAt());
        task.put("environment", s.getEnvironment() != null ? s.getEnvironment() : "dev");
        task.put("devScriptId", s.getDevScriptId());
        task.put("prodScriptId", s.getProdScriptId());
        return task;
    }

    private Map<String, Object> buildSyncTaskMap(DlSyncTask t) {
        Map<String, Object> task = new HashMap<>();
        task.put("id", t.getId());
        task.put("name", t.getTaskName());
        task.put("type", "syncTask");
        task.put("layer", t.getTaskLayer() != null ? t.getTaskLayer() : "ODS");
        task.put("scheduleStatus", t.getScheduleStatus());
        task.put("scheduleCron", t.getScheduleCron());
        task.put("sourceDb", t.getSourceDb());
        task.put("sourceTable", t.getSourceTable());
        task.put("targetTable", t.getTargetTable());
        task.put("updatedAt", t.getUpdatedAt());
        task.put("environment", "dev");
        return task;
    }

    @Operation(summary = "获取脚本详情（含完整内容）")
    @GetMapping("/script/{id}")
    public R<DlScript> getScript(@PathVariable Long id) {
        DlScript script = scriptMapper.selectById(id);
        if (script == null) {
            return R.fail("脚本不存在");
        }
        return R.ok(script);
    }

    @Operation(summary = "保存脚本")
    @PostMapping("/script/save")
    public R<DlScript> saveScript(@RequestBody DlScript script) {
        if (script.getId() == null) {
            script.setEnvironment("dev");
        }
        return R.ok(scriptService.save(script));
    }

    @Operation(summary = "删除脚本")
    @DeleteMapping("/script/{id}")
    public R<String> deleteScript(@PathVariable Long id) {
        scriptService.delete(id);
        return R.ok("删除成功");
    }

    // ======================== SQL 执行 ========================

    @Operation(summary = "执行SQL脚本")
    @PostMapping("/execute")
    public R<Map<String, Object>> executeSql(@RequestBody Map<String, Object> body,
                                              @AuthenticationPrincipal UserDetails user) {
        Long scriptId = body.get("scriptId") != null ? Long.valueOf(body.get("scriptId").toString()) : null;
        String sql = (String) body.get("sql");
        Long datasourceId = body.get("datasourceId") != null ? Long.valueOf(body.get("datasourceId").toString()) : null;
        Integer rowLimit = body.get("rowLimit") != null ? Integer.valueOf(body.get("rowLimit").toString()) : null;

        if (sql == null || sql.trim().isEmpty()) {
            return R.fail("SQL内容不能为空");
        }

        DlDatasource ds = resolveDatasource(datasourceId);
        if (ds == null) {
            return R.fail("没有可用的数据源，请先配置数据源");
        }

        String execSql = sql;
        if (rowLimit != null && rowLimit > 0) {
            execSql = applyRowLimit(sql, rowLimit);
        }

        Long userId = null;
        String username = null;
        if (user != null) {
            username = user.getUsername();
            DlUser dlUser = userMapper.selectOne(new QueryWrapper<DlUser>().eq("username", username));
            if (dlUser != null) userId = dlUser.getId();
        }

        return doExecuteSql(scriptId, execSql, ds, rowLimit, userId, username);
    }

    @Operation(summary = "安全执行SQL(自动应用数据权限)")
    @PostMapping("/execute-secure")
    public R<Map<String, Object>> executeSecureSql(@RequestBody Map<String, Object> body,
                                                    @AuthenticationPrincipal UserDetails user) {
        String sql = (String) body.get("sql");
        Long datasourceId = body.get("datasourceId") != null ? Long.valueOf(body.get("datasourceId").toString()) : null;
        String tableName = (String) body.get("tableName");
        Integer rowLimit = body.get("rowLimit") != null ? Integer.valueOf(body.get("rowLimit").toString()) : null;

        if (sql == null || sql.trim().isEmpty()) {
            return R.fail("SQL内容不能为空");
        }
        if (tableName == null || tableName.trim().isEmpty()) {
            return R.fail("请指定表名以应用数据权限");
        }

        DlDatasource ds = resolveDatasource(datasourceId);
        if (ds == null) {
            return R.fail("没有可用的数据源，请先配置数据源");
        }

        // 获取用户ID
        Long userId = null;
        String username = null;
        if (user != null) {
            username = user.getUsername();
            DlUser dlUser = userMapper.selectOne(new QueryWrapper<DlUser>().eq("username", username));
            if (dlUser != null) userId = dlUser.getId();
        }

        // 获取列级权限和行级过滤
        Map<String, String> colPerms = userId != null
                ? dataPermissionService.getColumnPermissions(userId, datasourceId, tableName)
                : Collections.emptyMap();
        String rowFilter = userId != null
                ? dataPermissionService.getRowFilter(userId, datasourceId, tableName)
                : null;

        // 应用行级过滤：对 SELECT 语句追加 WHERE 条件
        String execSql = sql;
        if (rowFilter != null && !rowFilter.isEmpty()) {
            String trimmed = sql.trim();
            if (trimmed.endsWith(";")) trimmed = trimmed.substring(0, trimmed.length() - 1);
            String upper = trimmed.toUpperCase();
            if (upper.startsWith("SELECT")) {
                if (upper.contains(" WHERE ")) {
                    execSql = trimmed + " AND (" + rowFilter + ")";
                } else {
                    execSql = trimmed + " WHERE " + rowFilter;
                }
            }
        }

        if (rowLimit != null && rowLimit > 0) {
            execSql = applyRowLimit(execSql, rowLimit);
        }

        R<Map<String, Object>> result = doExecuteSql(null, execSql, ds, rowLimit, userId, username);

        // 应用列级权限
        if (result.getData() != null && !colPerms.isEmpty()) {
            Object resultsObj = result.getData().get("results");
            if (resultsObj instanceof List) {
                List<?> results = (List<?>) resultsObj;
                for (Object obj : results) {
                    if (obj instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> r = (Map<String, Object>) obj;
                        if (r.containsKey("columns") && r.containsKey("rows")) {
                            @SuppressWarnings("unchecked")
                            List<String> cols = (List<String>) r.get("columns");
                            @SuppressWarnings("unchecked")
                            List<?> rows = (List<?>) r.get("rows");
                            Map<String, Object> applied = dataPermissionService.applyColumnPermissions(cols, rows, colPerms);
                            r.put("columns", applied.get("columns"));
                            r.put("rows", applied.get("rows"));
                            r.put("columnPermissionsApplied", true);
                        }
                    }
                }
            }
        }

        return result;
    }

    private DlDatasource resolveDatasource(Long datasourceId) {
        if (datasourceId != null) {
            DlDatasource ds = datasourceMapper.selectById(datasourceId);
            if (ds != null) return ds;
        }
        QueryWrapper<DlDatasource> qw = new QueryWrapper<>();
        qw.eq("type", "mysql").eq("status", 1).last("LIMIT 1");
        return datasourceMapper.selectList(qw).stream().findFirst().orElse(null);
    }

    private String applyRowLimit(String sql, int limit) {
        String trimmed = sql.trim();
        String upper = trimmed.toUpperCase();
        if (upper.startsWith("SELECT") && !upper.contains("LIMIT")) {
            // 去掉末尾分号再加LIMIT
            if (trimmed.endsWith(";")) {
                trimmed = trimmed.substring(0, trimmed.length() - 1);
            }
            return trimmed + " LIMIT " + limit;
        }
        return trimmed;
    }

    private R<Map<String, Object>> doExecuteSql(Long scriptId, String sql, DlDatasource ds, Integer rowLimit,
                                                 Long userId, String username) {
        long startTime = System.currentTimeMillis();
        Map<String, Object> result = new HashMap<>();
        String status = "SUCCESS";
        String errorMsg = null;
        long totalRows = 0;

        try (Connection conn = getConnection(ds);
             Statement stmt = conn.createStatement()) {

            String[] sqls = sql.split(";");
            List<Map<String, Object>> results = new ArrayList<>();

            for (String singleSql : sqls) {
                String trimmed = singleSql.trim();
                if (trimmed.isEmpty()) continue;

                String upperSql = trimmed.toUpperCase();
                boolean isQuery = upperSql.startsWith("SELECT") || upperSql.startsWith("SHOW")
                        || upperSql.startsWith("DESCRIBE") || upperSql.startsWith("DESC")
                        || upperSql.startsWith("EXPLAIN");

                if (isQuery) {
                    try (ResultSet rs = stmt.executeQuery(trimmed)) {
                        ResultSetMetaData meta = rs.getMetaData();
                        int colCount = meta.getColumnCount();

                        List<String> columns = new ArrayList<>();
                        for (int i = 1; i <= colCount; i++) {
                            columns.add(meta.getColumnName(i));
                        }

                        List<List<Object>> rows = new ArrayList<>();
                        int rowCount = 0;
                        int maxRows = rowLimit != null ? rowLimit : Constants.MAX_QUERY_ROWS;
                        while (rs.next() && rowCount < maxRows) {
                            List<Object> row = new ArrayList<>();
                            for (int i = 1; i <= colCount; i++) {
                                row.add(rs.getObject(i));
                            }
                            rows.add(row);
                            rowCount++;
                        }

                        Map<String, Object> queryResult = new HashMap<>();
                        queryResult.put("sql", trimmed.length() > 200 ? trimmed.substring(0, 200) + "..." : trimmed);
                        queryResult.put("columns", columns);
                        queryResult.put("rows", rows);
                        queryResult.put("rowCount", rowCount);
                        results.add(queryResult);
                        totalRows += rowCount;
                    }
                } else {
                    int affected = stmt.executeUpdate(trimmed);
                    Map<String, Object> updateResult = new HashMap<>();
                    updateResult.put("sql", trimmed.length() > 200 ? trimmed.substring(0, 200) + "..." : trimmed);
                    updateResult.put("affectedRows", affected);
                    results.add(updateResult);
                    totalRows += affected;
                }
            }

            long duration = System.currentTimeMillis() - startTime;
            result.put("success", true);
            result.put("results", results);
            result.put("totalRows", totalRows);
            result.put("durationMs", duration);
            result.put("datasource", ds.getName());

            if (scriptId != null) {
                recordExecution(scriptId, "script", sql, "SUCCESS", duration, totalRows, null);
            }

        } catch (Exception e) {
            log.error("SQL执行失败", e);
            long duration = System.currentTimeMillis() - startTime;
            status = "FAILED";
            errorMsg = e.getMessage();
            result.put("success", false);
            result.put("error", e.getMessage());
            result.put("durationMs", duration);

            if (scriptId != null) {
                recordExecution(scriptId, "script", sql, "FAILED", duration, -1, e.getMessage());
            }
        }

        long durationMs = System.currentTimeMillis() - startTime;
        sqlHistoryService.recordHistory(userId, username, ds.getId(), ds.getName(),
                ds.getDatabaseName(), sql, status, durationMs, totalRows, errorMsg, rowLimit, null);

        return R.ok(result);
    }

    private Connection getConnection(DlDatasource ds) throws SQLException {
        return connectionFactory.getConnection(ds);
    }

    private void recordExecution(Long taskId, String taskType, String sql, String status,
                                  long durationMs, long rowCount, String errorMsg) {
        try {
            DlTaskExecution exec = new DlTaskExecution();
            exec.setScriptId(taskId);
            exec.setTaskType(taskType);
            exec.setTriggerType("manual");
            exec.setStatus(status);
            exec.setStartTime(LocalDateTime.now().minusNanos(durationMs * 1_000_000));
            exec.setEndTime(LocalDateTime.now());
            exec.setDuration((int) (durationMs / 1000));
            exec.setLog(sql != null && sql.length() > 5000 ? sql.substring(0, 5000) : sql);
            exec.setCreatedAt(LocalDateTime.now());
            taskExecutionMapper.insert(exec);
        } catch (Exception e) {
            log.warn("记录执行历史失败", e);
        }
    }

    // ======================== 发布管理 ========================

    @Operation(summary = "发布脚本到生产环境（含灰度测试）")
    @PostMapping("/publish")
    public R<Map<String, Object>> publishScript(@RequestBody Map<String, Object> body) {
        Long devScriptId = Long.valueOf(body.get("devScriptId").toString());
        String publishComment = (String) body.getOrDefault("publishComment", "");
        String publishedBy = (String) body.getOrDefault("publishedBy", "admin");
        Boolean grayscaleEnabled = (Boolean) body.getOrDefault("grayscaleEnabled", true);
        Integer grayscaleLimit = body.get("grayscaleLimit") != null ?
                Integer.valueOf(body.get("grayscaleLimit").toString()) : 100;
        Long prodDatasourceId = body.get("prodDatasourceId") != null ?
                Long.valueOf(body.get("prodDatasourceId").toString()) : null;

        DlScript devScript = scriptMapper.selectById(devScriptId);
        if (devScript == null) {
            return R.fail("开发脚本不存在");
        }
        if (!"dev".equals(devScript.getEnvironment())) {
            return R.fail("只能发布开发环境的脚本");
        }

        // 生成发布版本号
        String version = generateVersion(devScriptId);

        // 创建发布记录
        DlScriptPublish publish = new DlScriptPublish();
        publish.setDevScriptId(devScriptId);
        publish.setPublishVersion(version);
        publish.setPublishType("normal");
        publish.setPublishStatus("pending");
        publish.setDevContent(devScript.getContent());
        publish.setGrayscaleEnabled(grayscaleEnabled ? 1 : 0);
        publish.setGrayscaleLimit(grayscaleLimit);
        publish.setPublishedBy(publishedBy);
        publish.setPublishComment(publishComment);
        publish.setCreatedAt(LocalDateTime.now());

        // 灰度测试
        Map<String, Object> grayscaleResult = null;
        if (grayscaleEnabled && devScript.getContent() != null && !devScript.getContent().trim().isEmpty()) {
            grayscaleResult = runGrayscaleTest(devScript, grayscaleLimit, prodDatasourceId);
            publish.setGrayscaleDevRows(grayscaleResult.get("devRows") != null ?
                    Long.valueOf(grayscaleResult.get("devRows").toString()) : null);
            publish.setGrayscaleProdRows(grayscaleResult.get("prodRows") != null ?
                    Long.valueOf(grayscaleResult.get("prodRows").toString()) : null);
            publish.setGrayscaleDevTime(grayscaleResult.get("devTime") != null ?
                    Long.valueOf(grayscaleResult.get("devTime").toString()) : null);
            publish.setGrayscaleProdTime(grayscaleResult.get("prodTime") != null ?
                    Long.valueOf(grayscaleResult.get("prodTime").toString()) : null);
            publish.setGrayscaleMatch(grayscaleResult.get("match") != null ?
                    (Boolean.TRUE.equals(grayscaleResult.get("match")) ? 1 : 0) : null);
            publish.setGrayscaleDetail(toJson(grayscaleResult));
            publish.setPublishStatus("grayscale_testing");
        }

        publishMapper.insert(publish);

        // 查找或创建生产环境脚本
        DlScript prodScript;
        if (devScript.getProdScriptId() != null) {
            prodScript = scriptMapper.selectById(devScript.getProdScriptId());
        } else {
            // 查找同名的prod脚本
            QueryWrapper<DlScript> qw = new QueryWrapper<>();
            qw.eq("script_name", devScript.getScriptName()).eq("environment", "prod");
            prodScript = scriptMapper.selectList(qw).stream().findFirst().orElse(null);
        }

        // 保存发布前prod内容快照（用于回滚）
        if (prodScript != null) {
            publish.setProdContentBefore(prodScript.getContent());
            publish.setProdScriptId(prodScript.getId());

            // 更新生产脚本
            prodScript.setContent(devScript.getContent());
            prodScript.setDescription(devScript.getDescription());
            prodScript.setDatabaseName(devScript.getDatabaseName());
            prodScript.setTaskLayer(devScript.getTaskLayer());
            prodScript.setScheduleCron(devScript.getScheduleCron());
            prodScript.setUpdatedAt(LocalDateTime.now());
            scriptMapper.updateById(prodScript);
        } else {
            // 创建新的生产脚本
            prodScript = new DlScript();
            prodScript.setScriptName(devScript.getScriptName());
            prodScript.setScriptType(devScript.getScriptType());
            prodScript.setContent(devScript.getContent());
            prodScript.setDescription(devScript.getDescription());
            prodScript.setDatabaseName(devScript.getDatabaseName());
            prodScript.setFolderId(devScript.getFolderId());
            prodScript.setTaskLayer(devScript.getTaskLayer());
            prodScript.setTaskStatus("online");
            prodScript.setEnvironment("prod");
            prodScript.setDevScriptId(devScriptId);
            prodScript.setScheduleCron(devScript.getScheduleCron());
            prodScript.setScheduleStatus(devScript.getScheduleStatus());
            prodScript.setTimeoutSeconds(devScript.getTimeoutSeconds());
            prodScript.setRetryTimes(devScript.getRetryTimes());
            prodScript.setRetryInterval(devScript.getRetryInterval());
            prodScript.setWarningType(devScript.getWarningType());
            prodScript.setOwner(devScript.getOwner());
            prodScript.setCreatedAt(LocalDateTime.now());
            prodScript.setUpdatedAt(LocalDateTime.now());
            scriptMapper.insert(prodScript);

            publish.setProdScriptId(prodScript.getId());
        }

        // 更新dev脚本的关联
        devScript.setProdScriptId(prodScript.getId());
        devScript.setTaskStatus("online");
        devScript.setUpdatedAt(LocalDateTime.now());
        scriptMapper.updateById(devScript);

        // 更新发布记录
        publish.setPublishStatus("published");
        publish.setPublishedAt(LocalDateTime.now());
        publishMapper.updateById(publish);

        Map<String, Object> result = new HashMap<>();
        result.put("publishId", publish.getId());
        result.put("version", version);
        result.put("devScriptId", devScriptId);
        result.put("prodScriptId", prodScript.getId());
        result.put("grayscaleResult", grayscaleResult);
        result.put("status", "published");
        return R.ok(result);
    }

    @Operation(summary = "灰度测试 — 用LIMIT采样对比dev和prod执行结果")
    @PostMapping("/grayscale-test")
    public R<Map<String, Object>> grayscaleTest(@RequestBody Map<String, Object> body) {
        Long devScriptId = Long.valueOf(body.get("devScriptId").toString());
        Integer grayscaleLimit = body.get("grayscaleLimit") != null ?
                Integer.valueOf(body.get("grayscaleLimit").toString()) : 100;
        Long prodDatasourceId = body.get("prodDatasourceId") != null ?
                Long.valueOf(body.get("prodDatasourceId").toString()) : null;

        DlScript devScript = scriptMapper.selectById(devScriptId);
        if (devScript == null) {
            return R.fail("脚本不存在");
        }

        Map<String, Object> result = runGrayscaleTest(devScript, grayscaleLimit, prodDatasourceId);
        return R.ok(result);
    }

    private Map<String, Object> runGrayscaleTest(DlScript devScript, int limit, Long prodDatasourceId) {
        Map<String, Object> result = new HashMap<>();
        result.put("grayscaleLimit", limit);

        if (devScript.getContent() == null || devScript.getContent().trim().isEmpty()) {
            result.put("match", false);
            result.put("error", "脚本内容为空");
            return result;
        }

        // 提取SELECT语句进行灰度测试
        String sql = devScript.getContent().trim();
        String upperSql = sql.toUpperCase();
        if (!upperSql.contains("SELECT")) {
            result.put("match", null);
            result.put("skipped", true);
            result.put("message", "非查询语句，跳过灰度测试");
            return result;
        }

        DlDatasource devDs = resolveDatasource(null);
        DlDatasource prodDs = prodDatasourceId != null ?
                resolveDatasource(prodDatasourceId) : devDs;

        try {
            // Dev环境执行
            long devStart = System.currentTimeMillis();
            R<Map<String, Object>> devResult = doExecuteSql(null, applyRowLimit(sql, limit), devDs, limit, null, null);
            long devTime = System.currentTimeMillis() - devStart;
            Map<String, Object> devData = devResult.getData();
            long devRows = devData.get("totalRows") != null ? Long.parseLong(devData.get("totalRows").toString()) : 0;

            result.put("devRows", devRows);
            result.put("devTime", devTime);
            result.put("devSuccess", Boolean.TRUE.equals(devData.get("success")));

            // Prod环境执行
            long prodStart = System.currentTimeMillis();
            R<Map<String, Object>> prodResult = doExecuteSql(null, applyRowLimit(sql, limit), prodDs, limit, null, null);
            long prodTime = System.currentTimeMillis() - prodStart;
            Map<String, Object> prodData = prodResult.getData();
            long prodRows = prodData.get("totalRows") != null ? Long.parseLong(prodData.get("totalRows").toString()) : 0;

            result.put("prodRows", prodRows);
            result.put("prodTime", prodTime);
            result.put("prodSuccess", Boolean.TRUE.equals(prodData.get("success")));

            // 对比结果
            boolean bothSuccess = Boolean.TRUE.equals(devData.get("success"))
                    && Boolean.TRUE.equals(prodData.get("success"));
            boolean rowsMatch = devRows == prodRows;
            result.put("match", bothSuccess && rowsMatch);

            if (!bothSuccess) {
                result.put("mismatchReason", "有一方执行失败");
            } else if (!rowsMatch) {
                result.put("mismatchReason", "返回行数不一致: dev=" + devRows + ", prod=" + prodRows);
            }

        } catch (Exception e) {
            log.error("灰度测试失败", e);
            result.put("match", false);
            result.put("error", e.getMessage());
        }

        return result;
    }

    @Operation(summary = "获取脚本发布历史")
    @GetMapping("/publish-history/{devScriptId}")
    public R<List<DlScriptPublish>> getPublishHistory(@PathVariable Long devScriptId) {
        QueryWrapper<DlScriptPublish> qw = new QueryWrapper<>();
        qw.eq("dev_script_id", devScriptId).orderByDesc("created_at");
        return R.ok(publishMapper.selectList(qw));
    }

    @Operation(summary = "获取全部发布历史(分页)")
    @GetMapping("/publish-history/all")
    public R<Map<String, Object>> getAllPublishHistory(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestParam(required = false) String environment,
            @RequestParam(required = false) String publishStatus,
            @RequestParam(required = false) String publishType) {
        Page<DlScriptPublish> page = new Page<>(pageNum, pageSize);
        QueryWrapper<DlScriptPublish> qw = new QueryWrapper<>();
        if (publishStatus != null && !publishStatus.isEmpty()) {
            qw.eq("publish_status", publishStatus);
        }
        if (publishType != null && !publishType.isEmpty()) {
            qw.eq("publish_type", publishType);
        }
        qw.orderByDesc("created_at");
        Page<DlScriptPublish> result = publishMapper.selectPage(page, qw);
        Map<String, Object> map = new HashMap<>();
        map.put("records", result.getRecords());
        map.put("total", result.getTotal());
        map.put("pageNum", pageNum);
        map.put("pageSize", pageSize);
        return R.ok(map);
    }

    @Operation(summary = "回滚生产脚本到指定发布版本")
    @PostMapping("/rollback/{publishId}")
    public R<Map<String, Object>> rollbackScript(@PathVariable Long publishId,
                                                  @RequestBody Map<String, Object> body) {
        DlScriptPublish publish = publishMapper.selectById(publishId);
        if (publish == null) {
            return R.fail("发布记录不存在");
        }
        if (publish.getProdScriptId() == null) {
            return R.fail("该发布记录没有关联的生产脚本");
        }

        DlScript prodScript = scriptMapper.selectById(publish.getProdScriptId());
        if (prodScript == null) {
            return R.fail("生产脚本不存在");
        }

        String rollbackContent = publish.getProdContentBefore();
        if (rollbackContent == null) {
            // 从dev脚本回滚prod
            DlScript devScript = scriptMapper.selectById(publish.getDevScriptId());
            if (devScript != null) {
                rollbackContent = devScript.getContent();
            }
        }
        if (rollbackContent == null) {
            return R.fail("没有可用的回滚内容");
        }

        String publishedBy = (String) body.getOrDefault("publishedBy", "admin");

        // 创建回滚发布记录
        DlScriptPublish rollbackPublish = new DlScriptPublish();
        rollbackPublish.setDevScriptId(publish.getDevScriptId());
        rollbackPublish.setProdScriptId(publish.getProdScriptId());
        rollbackPublish.setPublishVersion(generateVersion(publish.getDevScriptId()) + "-rollback");
        rollbackPublish.setPublishType("rollback");
        rollbackPublish.setPublishStatus("published");
        rollbackPublish.setDevContent(publish.getDevContent());
        rollbackPublish.setProdContentBefore(prodScript.getContent());
        rollbackPublish.setPublishedBy(publishedBy);
        rollbackPublish.setPublishComment("回滚到版本 " + publish.getPublishVersion());
        rollbackPublish.setPublishedAt(LocalDateTime.now());
        rollbackPublish.setCreatedAt(LocalDateTime.now());
        publishMapper.insert(rollbackPublish);

        // 执行回滚
        prodScript.setContent(rollbackContent);
        prodScript.setUpdatedAt(LocalDateTime.now());
        scriptMapper.updateById(prodScript);

        // 更新原发布记录状态
        publish.setPublishStatus("rolled_back");
        publishMapper.updateById(publish);

        Map<String, Object> result = new HashMap<>();
        result.put("rollbackPublishId", rollbackPublish.getId());
        result.put("prodScriptId", prodScript.getId());
        result.put("status", "rolled_back");
        return R.ok(result);
    }

    @Operation(summary = "对比dev和prod脚本内容")
    @GetMapping("/diff/{devScriptId}")
    public R<Map<String, Object>> getScriptDiff(@PathVariable Long devScriptId) {
        DlScript devScript = scriptMapper.selectById(devScriptId);
        if (devScript == null) {
            return R.fail("开发脚本不存在");
        }

        Map<String, Object> result = new HashMap<>();
        result.put("devScriptId", devScriptId);
        result.put("devContent", devScript.getContent());
        result.put("devUpdatedAt", devScript.getUpdatedAt());

        if (devScript.getProdScriptId() != null) {
            DlScript prodScript = scriptMapper.selectById(devScript.getProdScriptId());
            if (prodScript != null) {
                result.put("prodScriptId", prodScript.getId());
                result.put("prodContent", prodScript.getContent());
                result.put("prodUpdatedAt", prodScript.getUpdatedAt());
                boolean same = Objects.equals(devScript.getContent(), prodScript.getContent());
                result.put("isSame", same);
            }
        } else {
            result.put("prodScriptId", null);
            result.put("isSame", false);
            result.put("prodStatus", "not_published");
        }

        return R.ok(result);
    }

    // ======================== 依赖管理 ========================

    @Operation(summary = "获取任务依赖关系")
    @GetMapping("/dependencies/{taskId}")
    public R<Map<String, Object>> getDependencies(@PathVariable Long taskId,
                                                   @RequestParam(defaultValue = "script") String taskType) {
        Map<String, Object> result = new HashMap<>();

        QueryWrapper<DlTaskDependency> upstreamQw = new QueryWrapper<>();
        upstreamQw.eq("task_id", taskId).eq("task_type", taskType);
        List<DlTaskDependency> upstream = depMapper.selectList(upstreamQw);

        QueryWrapper<DlTaskDependency> downstreamQw = new QueryWrapper<>();
        downstreamQw.eq("upstream_task_id", taskId).eq("upstream_task_type", taskType);
        List<DlTaskDependency> downstream = depMapper.selectList(downstreamQw);

        List<Map<String, Object>> upstreamList = enrichDependencies(upstream, true);
        List<Map<String, Object>> downstreamList = enrichDependencies(downstream, false);

        result.put("upstream", upstreamList);
        result.put("downstream", downstreamList);

        return R.ok(result);
    }

    private List<Map<String, Object>> enrichDependencies(List<DlTaskDependency> deps, boolean isUpstream) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (DlTaskDependency dep : deps) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", dep.getId());
            item.put("depTable", dep.getDepTable());

            Long relatedTaskId = isUpstream ? dep.getUpstreamTaskId() : dep.getTaskId();
            String relatedTaskType = isUpstream ? dep.getUpstreamTaskType() : dep.getTaskType();

            item.put("relatedTaskId", relatedTaskId);
            item.put("relatedTaskType", relatedTaskType);

            if (Constants.TASK_TYPE_SCRIPT.equals(relatedTaskType)) {
                DlScript script = scriptMapper.selectById(relatedTaskId);
                item.put("relatedTaskName", script != null ? script.getScriptName() : "未知脚本");
                item.put("relatedTaskLayer", script != null ? script.getTaskLayer() : "");
                item.put("relatedTaskEnv", script != null ? script.getEnvironment() : "");
            } else {
                DlSyncTask syncTask = syncTaskMapper.selectById(relatedTaskId);
                item.put("relatedTaskName", syncTask != null ? syncTask.getTaskName() : "未知同步任务");
                item.put("relatedTaskLayer", syncTask != null ? syncTask.getTaskLayer() : "");
            }

            list.add(item);
        }
        return list;
    }

    @Operation(summary = "刷新所有依赖关系")
    @PostMapping("/dependencies/refresh")
    public R<Map<String, Object>> refreshDependencies() {
        int count = dependencyService.refreshAllDependencies();
        Map<String, Object> result = new HashMap<>();
        result.put("updatedCount", count);
        return R.ok(result);
    }

    // ======================== 调度配置 ========================

    @Operation(summary = "更新任务调度配置")
    @PutMapping("/schedule/{taskId}")
    public R<String> updateSchedule(@PathVariable Long taskId,
                                     @RequestParam(defaultValue = "script") String taskType,
                                     @RequestBody Map<String, Object> body) {
        String scheduleCron = (String) body.get("scheduleCron");
        String scheduleStatus = (String) body.get("scheduleStatus");
        Integer timeoutSeconds = body.get("timeoutSeconds") != null ?
                Integer.valueOf(body.get("timeoutSeconds").toString()) : null;
        Integer retryTimes = body.get("retryTimes") != null ?
                Integer.valueOf(body.get("retryTimes").toString()) : null;
        Integer retryInterval = body.get("retryInterval") != null ?
                Integer.valueOf(body.get("retryInterval").toString()) : null;
        String warningType = (String) body.get("warningType");

        if (Constants.TASK_TYPE_SCRIPT.equals(taskType)) {
            DlScript script = new DlScript();
            script.setId(taskId);
            if (scheduleCron != null) script.setScheduleCron(scheduleCron);
            if (scheduleStatus != null) script.setScheduleStatus(scheduleStatus);
            if (timeoutSeconds != null) script.setTimeoutSeconds(timeoutSeconds);
            if (retryTimes != null) script.setRetryTimes(retryTimes);
            if (retryInterval != null) script.setRetryInterval(retryInterval);
            if (warningType != null) script.setWarningType(warningType);
            script.setUpdatedAt(LocalDateTime.now());
            scriptMapper.updateById(script);
        } else {
            DlSyncTask task = new DlSyncTask();
            task.setId(taskId);
            if (scheduleCron != null) task.setScheduleCron(scheduleCron);
            if (scheduleStatus != null) task.setScheduleStatus(scheduleStatus);
            if (timeoutSeconds != null) task.setTimeoutSeconds(timeoutSeconds);
            if (retryTimes != null) task.setRetryTimes(retryTimes);
            if (retryInterval != null) task.setRetryInterval(retryInterval);
            if (warningType != null) task.setWarningType(warningType);
            task.setUpdatedAt(LocalDateTime.now());
            syncTaskMapper.updateById(task);
        }

        return R.ok("调度配置更新成功");
    }

    @Operation(summary = "获取任务执行历史")
    @GetMapping("/executions/{taskId}")
    public R<List<DlTaskExecution>> getExecutions(@PathVariable Long taskId,
                                                   @RequestParam(defaultValue = "script") String taskType,
                                                   @RequestParam(defaultValue = "20") int limit) {
        QueryWrapper<DlTaskExecution> qw = new QueryWrapper<>();
        qw.eq("script_id", taskId).eq("task_type", taskType);
        qw.orderByDesc("start_time").last("LIMIT " + limit);
        return R.ok(taskExecutionMapper.selectList(qw));
    }

    // ======================== 统计概览 ========================

    @Operation(summary = "获取数据开发概览统计")
    @GetMapping("/overview")
    public R<Map<String, Object>> overview() {
        Map<String, Object> result = new HashMap<>();

        // 各层任务数
        List<DlScript> allScripts = scriptMapper.selectList(null);
        List<DlSyncTask> allSyncTasks = syncTaskMapper.selectList(null);

        Map<String, Integer> layerCount = new LinkedHashMap<>();
        layerCount.put("ODS", 0);
        layerCount.put("DWD", 0);
        layerCount.put("DWS", 0);
        layerCount.put("ADS", 0);
        layerCount.put("DIM", 0);

        for (DlScript s : allScripts) {
            String layer = s.getTaskLayer() != null ? s.getTaskLayer() : "ODS";
            layerCount.merge(layer, 1, Integer::sum);
        }
        for (DlSyncTask t : allSyncTasks) {
            String layer = t.getTaskLayer() != null ? t.getTaskLayer() : "ODS";
            layerCount.merge(layer, 1, Integer::sum);
        }

        result.put("layerCount", layerCount);
        result.put("totalTasks", allScripts.size() + allSyncTasks.size());

        // 环境统计
        long devScripts = allScripts.stream().filter(s -> "dev".equals(s.getEnvironment())).count();
        long prodScripts = allScripts.stream().filter(s -> "prod".equals(s.getEnvironment())).count();
        result.put("devScripts", devScripts);
        result.put("prodScripts", prodScripts);

        // 今日执行统计
        LocalDate today = LocalDate.now();
        QueryWrapper<DlTaskExecution> todayQw = new QueryWrapper<>();
        todayQw.ge("start_time", today.atStartOfDay());
        todayQw.le("start_time", today.plusDays(1).atStartOfDay());
        List<DlTaskExecution> todayExecs = taskExecutionMapper.selectList(todayQw);

        long todaySuccess = todayExecs.stream().filter(e -> "SUCCESS".equals(e.getStatus())).count();
        long todayFailed = todayExecs.stream().filter(e -> "FAILED".equals(e.getStatus())).count();

        result.put("todayExecutions", todayExecs.size());
        result.put("todaySuccess", todaySuccess);
        result.put("todayFailed", todayFailed);

        long scheduledScripts = allScripts.stream()
                .filter(s -> "online".equals(s.getScheduleStatus())).count();
        long scheduledSyncTasks = allSyncTasks.stream()
                .filter(t -> "online".equals(t.getScheduleStatus())).count();
        result.put("scheduledTasks", scheduledScripts + scheduledSyncTasks);

        return R.ok(result);
    }

    // ======================== 文件夹管理 ========================

    @Operation(summary = "获取脚本文件夹树")
    @GetMapping("/folders")
    public R<List<Map<String, Object>>> getFolders() {
        return R.ok(scriptService.getTree());
    }

    // ======================== 工具方法 ========================

    private String generateVersion(Long devScriptId) {
        QueryWrapper<DlScriptPublish> qw = new QueryWrapper<>();
        qw.eq("dev_script_id", devScriptId).orderByDesc("id").last("LIMIT 1");
        DlScriptPublish last = publishMapper.selectList(qw).stream().findFirst().orElse(null);
        if (last != null && last.getPublishVersion() != null) {
            String v = last.getPublishVersion().replaceAll("-rollback$", "");
            String[] parts = v.replace("v", "").split("\\.");
            if (parts.length == 3) {
                try {
                    int patch = Integer.parseInt(parts[2]) + 1;
                    return "v" + parts[0] + "." + parts[1] + "." + patch;
                } catch (NumberFormatException ignored) {}
            }
        }
        return "v1.0.0";
    }

    private String toJson(Object obj) {
        if (obj == null) return null;
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(obj);
        } catch (Exception e) {
            return obj.toString();
        }
    }
}