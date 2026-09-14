package com.datalink.controller;

import com.datalink.mapper.DlDatasourceMapper;
import com.datalink.mapper.DlSyncTaskMapper;
import com.datalink.mapper.DlTaskExecutionMapper;
import com.datalink.model.ColumnInfo;
import com.datalink.model.DlDatasource;
import com.datalink.model.DlSyncTask;
import com.datalink.model.DlTaskExecution;
import com.datalink.model.R;
import com.datalink.model.dto.DataxCreateAndSyncRequest;
import com.datalink.model.dto.DataxGenerateJobRequest;
import com.datalink.model.dto.DataxRunRequest;
import com.datalink.service.DataxService;
import com.datalink.service.HiveService;
import com.datalink.service.MetadataService;
import com.datalink.util.ProcessUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DataX 同步管理 Controller
 */
@Slf4j
@RestController
@Tag(name = "DataX 数据同步", description = "MySQL 到 Hive 的数据同步管理")
@RequestMapping("/api/datax")
@RequiredArgsConstructor
public class DataxController {

    private final DataxService dataxService;
    private final MetadataService metadataService;
    private final HiveService hiveService;
    private final DlDatasourceMapper datasourceMapper;
    private final DlSyncTaskMapper syncTaskMapper;
    private final DlTaskExecutionMapper taskExecutionMapper;

    @Value("${spring.datasource.url:}")
    private String defaultDbUrl;

    @Value("${spring.datasource.username:root}")
    private String defaultDbUser;

    @Value("${spring.datasource.password:}")
    private String defaultDbPass;

    @Operation(summary = "同步任务列表")
    @GetMapping("/sync-tasks")
    public R<List<DlSyncTask>> listSyncTasks() {
        return R.ok(syncTaskMapper.selectList(null));
    }

    @Operation(summary = "创建同步任务")
    @PostMapping("/sync-task/create")
    public R<DlSyncTask> createSyncTask(@RequestBody DlSyncTask task) {
        syncTaskMapper.insert(task);
        return R.ok(task);
    }

    @Operation(summary = "更新同步任务")
    @PostMapping("/sync-task/update")
    public R<String> updateSyncTask(@RequestBody DlSyncTask task) {
        syncTaskMapper.updateById(task);
        return R.ok("更新成功");
    }

    @Operation(summary = "删除同步任务")
    @PostMapping("/sync-task/delete/{id}")
    public R<String> deleteSyncTask(@PathVariable Long id) {
        syncTaskMapper.deleteById(id);
        return R.ok("删除成功");
    }

    /**
     * 生成 DataX JSON 配置（纯内存，不写磁盘）
     */
    @Operation(summary = "生成 DataX 任务配置")
    @PostMapping("/generate-job")
    public R<Map<String, String>> generateJob(@RequestBody DataxGenerateJobRequest body) {
        try {
            String db = body.getDb();
            String table = body.getTable();
            String syncMode = body.getSyncMode() != null ? body.getSyncMode() : "df";
            DlDatasource ds = resolveDatasource(body.getDatasourceId());

            List<ColumnInfo> columns = metadataService.getColumns(db, table);
            String odsTable = hiveService.getOdsTableName(db, table, syncMode);
            String jobJson = dataxService.generateJobJsonString(
                    ds.getHost(), ds.getPort(), ds.getUsername(), ds.getPassword(),
                    db, table, odsTable, columns);

            Map<String, String> result = new HashMap<>();
            result.put("jobJson", jobJson);
            result.put("odsTable", odsTable);
            return R.ok(result);
        } catch (Exception e) {
            log.error("生成 DataX 任务配置失败", e);
            return R.fail("生成任务配置失败");
        }
    }

    /**
     * 执行 DataX 同步任务（从数据库读取 JSON 配置，不依赖磁盘文件）
     */
    @Operation(summary = "执行 DataX 同步任务")
    @PostMapping("/run")
    public R<Map<String, Object>> run(@RequestBody DataxRunRequest body) {
        try {
            DlSyncTask task = syncTaskMapper.selectById(body.getSyncTaskId());
            if (task == null) return R.fail("同步任务不存在: " + body.getSyncTaskId());
            if (task.getDataxJson() == null || task.getDataxJson().isEmpty()) {
                return R.fail("任务尚未生成 DataX 配置，请先触发一次同步");
            }

            ProcessUtil.ExecResult execResult = dataxService.runJobInMemory(task.getDataxJson(), task.getTargetTable());

            Map<String, Object> data = new HashMap<>();
            data.put("exitCode", execResult.getExitCode());
            data.put("durationMs", execResult.getDurationMs());
            data.put("output", execResult.getOutput());
            data.put("success", execResult.getExitCode() == 0);
            return R.ok(data);
        } catch (Exception e) {
            log.error("执行 DataX 同步任务失败", e);
            return R.fail("执行同步任务失败");
        }
    }

    /**
     * 一键建表并同步
     */
    @Operation(summary = "一键建表并同步数据")
    @PostMapping("/create-and-sync")
    public R<Map<String, Object>> createAndSync(@RequestBody DataxCreateAndSyncRequest body) {
        long startMs = System.currentTimeMillis();
        // 创建执行记录
        DlTaskExecution exec = new DlTaskExecution();
        exec.setSyncTaskId(body.getSyncTaskId());
        exec.setTaskType("syncTask");
        exec.setTriggerType("manual");
        exec.setStatus("RUNNING");
        exec.setStartTime(java.time.LocalDateTime.now());
        if (body.getSyncTaskId() != null) {
            taskExecutionMapper.insert(exec);
        }

        try {
            String db = body.getDb();
            String table = body.getTable();
            String syncMode = body.getSyncMode() != null ? body.getSyncMode() : "df";
            DlDatasource ds = resolveDatasource(body.getDatasourceId());

            List<ColumnInfo> columns = metadataService.getColumns(db, table);
            String odsTable = hiveService.getOdsTableName(db, table, syncMode);

            String ddl = hiveService.generateDDL(db, table, columns, syncMode);
            hiveService.executeDDL(ddl);

            String today = java.time.LocalDate.now().minusDays(1).toString();
            String addPartitionSql = "ALTER TABLE ods." + odsTable
                    + " ADD IF NOT EXISTS PARTITION (dt='" + today + "')";
            hiveService.executeDDL(addPartitionSql);

            String jobJson = dataxService.generateJobJsonString(
                    ds.getHost(), ds.getPort(), ds.getUsername(), ds.getPassword(),
                    db, table, odsTable, columns);

            // 如果有关联任务，把生成的配置保存回数据库
            if (body.getSyncTaskId() != null) {
                DlSyncTask jsonUpdate = new DlSyncTask();
                jsonUpdate.setId(body.getSyncTaskId());
                jsonUpdate.setDataxJson(jobJson);
                syncTaskMapper.updateById(jsonUpdate);
            }

            ProcessUtil.ExecResult execResult = dataxService.runJobInMemory(jobJson, odsTable);

            // 更新执行记录
            if (exec.getId() != null) {
                exec.setStatus(execResult.getExitCode() == 0 ? "SUCCESS" : "FAILED");
                exec.setEndTime(java.time.LocalDateTime.now());
                exec.setDuration((int)((System.currentTimeMillis() - startMs) / 1000));
                exec.setLog(execResult.getOutput() != null && execResult.getOutput().length() > 50000
                        ? execResult.getOutput().substring(execResult.getOutput().length() - 50000)
                        : execResult.getOutput());
                taskExecutionMapper.updateById(exec);
            }

            Map<String, Object> data = new HashMap<>();
            data.put("odsTable", odsTable);
            data.put("ddl", ddl);
            data.put("exitCode", execResult.getExitCode());
            data.put("durationMs", execResult.getDurationMs());
            data.put("success", execResult.getExitCode() == 0);
            data.put("output", execResult.getOutput());
            return R.ok(data);
        } catch (Exception e) {
            log.error("一键建表并同步失败", e);
            // 更新执行记录为失败
            if (exec.getId() != null) {
                exec.setStatus("FAILED");
                exec.setEndTime(java.time.LocalDateTime.now());
                exec.setDuration((int)((System.currentTimeMillis() - startMs) / 1000));
                exec.setLog(e.getMessage());
                taskExecutionMapper.updateById(exec);
            }
            String errMsg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            return R.fail("建表同步操作失败: " + errMsg);
        }
    }

    private DlDatasource resolveDatasource(String dsIdStr) {
        if (dsIdStr != null && !dsIdStr.isEmpty()) {
            try {
                DlDatasource ds = datasourceMapper.selectById(Long.valueOf(dsIdStr));
                if (ds != null) return ds;
            } catch (NumberFormatException ignored) {}
        }
        return getDefaultDatasource();
    }

    private DlDatasource getDefaultDatasource() {
        DlDatasource ds = new DlDatasource();
        try {
            String hostPort = defaultDbUrl.split("//")[1].split("/")[0];
            ds.setHost(hostPort.split(":")[0]);
            ds.setPort(Integer.parseInt(hostPort.split(":")[1]));
        } catch (Exception e) {
            ds.setHost("127.0.0.1");
            ds.setPort(3306);
        }
        ds.setUsername(defaultDbUser);
        ds.setPassword(defaultDbPass);
        return ds;
    }
}